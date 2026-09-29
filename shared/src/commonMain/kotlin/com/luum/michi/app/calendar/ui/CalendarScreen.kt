package com.luum.michi.app.calendar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.calendar.ui.components.CalendarFilterSheet
import com.luum.michi.app.calendar.ui.components.CalendarReleaseRow
import com.luum.michi.app.calendar.ui.state.CalendarStateHolder
import com.luum.michi.app.calendar.domain.model.CalendarSeasonFilter
import com.luum.michi.app.calendar.domain.model.CalendarStatusFilter
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.model.utcMillisToLocalMidnightBucket
import com.luum.michi.app.calendar.ui.components.CalendarDateBar
import com.luum.michi.app.ui.components.DatePickerField
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.PullRefresh
import com.luum.michi.app.ui.components.TabFadeMs
import com.luum.michi.app.ui.components.tabFadeSpec
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Airing calendar: releases of the selected date over a floating bottom
 * date bar (Otraku parity: [<] [date] [>] — the date opens the system
 * calendar, arrows step ±1 day with empty days included). States reuse
 * the app-wide vocabulary (spinner, [MessagePanel] with retry, empty
 * label). The feed is public, so guests render the same surface with
 * null user fields. Release taps navigate to detail (dormant until
 * detail nav lands, same as the list cards).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarScreen(
    holder: CalendarStateHolder,
    onRetry: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
    // Scaffold bottom threaded from Root: the last item clears the floating
    // date bar with the same air the TabBar keeps.
    bottomPadding: Dp = 0.dp,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    // Filter sheet: Root owns the open flag so the toolbar back affordance
    // and system back close the sheet before the overlay.
    isFilterOpen: Boolean = false,
    onDismissFilter: () -> Unit = {},
    onOpenDetail: (Int) -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
) {
    val items = holder.selectedItems
    val selected = holder.selectedDayBucket
    val minBucket = holder.minDayBucket
    val maxBucket = holder.maxDayBucket
    val selectableDates = remember(minBucket, maxBucket) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val bucket = utcTimeMillis.millisToBucket()
                return bucket in minBucket..maxBucket
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                holder.isLoading && items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = strings.listsLoadingLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                holder.error != null && items.isEmpty() -> {
                    MessagePanel(
                        title = strings.listsErrorLabel,
                        message = holder.error?.let { strings.networkErrorMessage(it) },
                        icon = Icons.Filled.Warning,
                        actionLabel = strings.retryAction,
                        onAction = onRetry,
                    )
                }
                items.isEmpty() -> {
                    MessagePanel(
                        title = strings.calendarEmptyLabel,
                        message = null,
                        icon = AppIcons.Calendar,
                        actionLabel = null,
                        onAction = {},
                    )
                }
                else -> {
                    PullRefresh(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                top = 16.dp,
                                bottom = 16.dp + bottomPadding + BottomBarClearance,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = items,
                                key = { it.scheduleId },
                            ) { entry ->
                                CalendarReleaseRow(
                                    entry = entry,
                                    onOpenDetail = onOpenDetail,
                                    onOpenUrl = onOpenUrl,
                                )
                            }
                        }
                    }
                }
            }
        }
        // Deferred entrance: the TabBar exits over TabFadeMs when the overlay
        // opens, so the date bar waits it out instead of fighting for the
        // same space mid-animation (it would ride up, then settle alone).
        var barReady by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(TabFadeMs.milliseconds)
            barReady = true
        }
        // Same enter/exit language as the TabBar (see Root): the bar
        // slides up with a fade instead of popping in after load.
        AnimatedVisibility(
            visible = barReady && holder.hasLoadedDays && selected != null,
            enter = slideInVertically(animationSpec = tabFadeSpec()) { it } +
                fadeIn(animationSpec = tabFadeSpec()),
            exit = slideOutVertically(animationSpec = tabFadeSpec()) { it } +
                fadeOut(animationSpec = tabFadeSpec()),
            label = "date-bar-visibility",
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val current = selected
            if (current != null) {
                CalendarDateBar(
                    onPrevious = { holder.stepDay(-1) },
                    onNext = { holder.stepDay(1) },
                    previousEnabled = current > minBucket,
                    nextEnabled = current < maxBucket,
                ) {
                    DatePickerField(
                        millis = current.bucketToMillis(),
                        onConfirm = { holder.selectDay(it.millisToBucket()) },
                        onClear = { holder.selectDay(holder.todayBucket) },
                        buttonLabel = dateButtonLabel(current, holder.todayBucket, strings),
                        selectableDates = selectableDates,
                        strings = strings,
                        // Trims the date pill to sit even with the circles.
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                    )
                }
            }
        }
    }
    if (isFilterOpen) {
        CalendarFilterSheet(
            season = holder.seasonFilter,
            onSelectSeason = { holder.updateSeasonFilter(it) },
            status = holder.statusFilter,
            onSelectStatus = { holder.updateStatusFilter(it) },
            onReset = {
                holder.updateSeasonFilter(CalendarSeasonFilter.ALL)
                holder.updateStatusFilter(CalendarStatusFilter.ALL)
            },
            onDismiss = onDismissFilter,
            strings = strings,
        )
    }
}

/** Date bar clearance: TabBar-sized capsule + its 24dp margin, above the tab-bar pad. */
private val BottomBarClearance = 96.dp

private fun Long.bucketToMillis(): Long = this * 1000

private fun Long.millisToBucket(): Long =
    utcMillisToLocalMidnightBucket(this, TimeZone.currentSystemDefault())

private fun dateButtonLabel(bucket: Long, todayBucket: Long, strings: LanguageStrings): String {
    val zone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochSeconds(bucket).toLocalDateTime(zone).date
    val today = Instant.fromEpochSeconds(todayBucket).toLocalDateTime(zone).date
    val prefix = when (today.daysUntil(date)) {
        0 -> strings.todayLabel
        1 -> strings.tomorrowLabel
        else -> dayName(date.dayOfWeek.isoDayNumber, strings)
    }
    return strings.calendarHeaderLabel(prefix, date.day, date.month.number, date.year)
}

private fun dayName(isoDayOfWeek: Int, strings: LanguageStrings): String = when (isoDayOfWeek) {
    1 -> strings.dayMonday
    2 -> strings.dayTuesday
    3 -> strings.dayWednesday
    4 -> strings.dayThursday
    5 -> strings.dayFriday
    6 -> strings.daySaturday
    else -> strings.daySunday
}
