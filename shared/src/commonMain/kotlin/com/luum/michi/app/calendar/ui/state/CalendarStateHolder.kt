package com.luum.michi.app.calendar.ui.state

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.luum.michi.app.calendar.domain.CalendarDay
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.domain.model.CalendarSeasonFilter
import com.luum.michi.app.calendar.domain.model.CalendarStatusFilter
import com.luum.michi.app.calendar.domain.model.matches
import com.luum.michi.app.core.model.MediaSeasonYear
import com.luum.michi.app.core.model.currentSeasonAndYear
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult

internal class CalendarStateHolder(
    private val repository: CalendarRepository,
    private val scope: CoroutineScope,
    private val currentSeason: () -> MediaSeasonYear = { currentSeasonAndYear() },
) {
    private var daysState: List<CalendarDay> = emptyList()
    private var loadingState = false
    private var errorState: NetworkError? = null

    var seasonFilter = CalendarSeasonFilter.ALL
        private set
    var statusFilter = CalendarStatusFilter.ALL
        private set
    var selectedDayBucket: Long? = null
        private set

    val days: List<CalendarDay> get() = daysState
    val isLoading: Boolean get() = loadingState
    val error: NetworkError? get() = errorState

    /** Days with filters applied; days left empty by filtering are dropped. */
    val visibleDays: List<CalendarDay>
        get() {
            val season = seasonFilter
            val status = statusFilter
            if (season == CalendarSeasonFilter.ALL && status == CalendarStatusFilter.ALL) return daysState
            val current = currentSeason()
            return daysState.mapNotNull { day ->
                val items = day.items.filter { entry ->
                    val item = entry.item
                    season.matches(item.season, item.seasonYear, current) &&
                        status.matches(item.userStatus, item.season, item.seasonYear)
                }
                if (items.isEmpty()) null else day.copy(items = items)
            }
        }

    fun updateSeasonFilter(filter: CalendarSeasonFilter) {
        seasonFilter = filter
    }

    fun updateStatusFilter(filter: CalendarStatusFilter) {
        statusFilter = filter
    }

    fun selectDay(dayBucket: Long?) {
        selectedDayBucket = dayBucket
    }

    /** Moves the day selection within the *visible* days, clamped to range; no-op when empty. */
    fun stepDay(delta: Int) {
        val buckets = visibleDays.map { it.dayBucket }.sorted()
        if (buckets.isEmpty()) return
        val current = selectedDayBucket?.takeIf { it in buckets } ?: buckets.first()
        val next = (buckets.indexOf(current) + delta).coerceIn(buckets.indices)
        selectedDayBucket = buckets[next]
    }

    fun load() {
        // Drop overlapping loads so a slower (stale) emission cannot overwrite a newer one.
        if (loadingState) return
        loadingState = true
        errorState = null
        scope.launch {
            repository.loadFeed()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    if (daysState.isEmpty()) errorState = NetworkError.Unknown(cause)
                    loadingState = false
                }
                .collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            daysState = result.value.days
                            if (selectedDayBucket == null ||
                                daysState.none { it.dayBucket == selectedDayBucket }
                            ) {
                                selectedDayBucket = daysState
                                    .firstOrNull { it.offsetFromToday == 0 }?.dayBucket
                                    ?: daysState.firstOrNull()?.dayBucket
                            }
                            loadingState = false
                        }
                        is NetworkResult.Failure -> {
                            if (daysState.isEmpty()) errorState = result.error
                            loadingState = false
                        }
                    }
                }
        }
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createCalendarStateHolder(
    repository: CalendarRepository,
    scope: CoroutineScope,
): CalendarStateHolder {
    return CalendarStateHolder(repository, scope)
}
