package com.luum.michi.app.calendar.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.calendar.data.CalendarDay
import com.luum.michi.app.calendar.presentation.components.CalendarItemRow
import com.luum.michi.app.calendar.presentation.state.CalendarStateHolder
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone

@Composable
internal fun CalendarScreen(
    stateHolder: CalendarStateHolder,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings

    androidx.compose.runtime.LaunchedEffect(stateHolder) {
        if (stateHolder.days.isEmpty() && !stateHolder.isLoading) {
            stateHolder.load()
        }
    }

    when {
        stateHolder.isLoading && stateHolder.days.isEmpty() -> PlatformListLoading(strings.listsLoadingLabel)
        stateHolder.error != null && stateHolder.days.isEmpty() -> PlatformListMessage(
            title = strings.listsErrorLabel,
            subtitle = stateHolder.error?.let { strings.networkErrorMessage(it) },
            tone = PlatformListMessageTone.Error,
        )
        stateHolder.days.isEmpty() -> PlatformListMessage(title = strings.calendarEmptyLabel)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            stateHolder.days.forEach { day ->
                item(key = "header-${day.dayBucket}") {
                    Text(
                        text = day.label(strings),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(items = day.items, key = { it.scheduleId }) { entry ->
                    val mediaId = entry.item.id
                    CalendarItemRow(
                        item = entry.item,
                        onClick = { if (mediaId != null) onOpenMedia(mediaId) },
                        onLongClick = { if (mediaId != null) onEditMedia(mediaId) },
                    )
                }
            }
        }
    }
}

private fun CalendarDay.label(strings: LanguageStrings): String {
    val prefix = when {
        offsetFromToday == 0 -> strings.todayLabel
        offsetFromToday == 1 -> strings.tomorrowLabel
        else -> dayOfWeekLabel(isoDayOfWeek, strings)
    }
    return strings.calendarHeaderLabel(prefix, day, month, year)
}

private fun dayOfWeekLabel(isoDayOfWeek: Int, strings: LanguageStrings): String = when (isoDayOfWeek) {
    1 -> strings.dayMonday
    2 -> strings.dayTuesday
    3 -> strings.dayWednesday
    4 -> strings.dayThursday
    5 -> strings.dayFriday
    6 -> strings.daySaturday
    7 -> strings.daySunday
    else -> ""
}
