package com.luum.michi.app.calendar.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import com.luum.michi.app.calendar.domain.CalendarEntry
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.domain.model.CalendarSeasonFilter
import com.luum.michi.app.calendar.domain.model.CalendarStatusFilter
import com.luum.michi.app.calendar.domain.model.matches
import com.luum.michi.app.core.auth.domain.currentEpochSeconds
import com.luum.michi.app.core.model.MediaSeasonYear
import com.luum.michi.app.core.model.currentSeasonAndYear
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult

internal class CalendarStateHolder(
    private val repository: CalendarRepository,
    private val scope: CoroutineScope,
    private val currentSeason: () -> MediaSeasonYear = { currentSeasonAndYear() },
    private val nowProvider: () -> Long = { currentEpochSeconds() },
    private val timeZoneProvider: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    /** Loaded day slices by local-midnight bucket. Plain map: reads go
     * through the state-backed views below, writes replace the reference. */
    private var cacheState: Map<Long, List<CalendarEntry>> by mutableStateOf(emptyMap())
    private var loadingBucketsState: Set<Long> by mutableStateOf(emptySet())
    private var errorState: NetworkError? by mutableStateOf(null)

    var seasonFilter by mutableStateOf(CalendarSeasonFilter.ALL)
        private set
    var statusFilter by mutableStateOf(CalendarStatusFilter.ALL)
        private set
    var selectedDayBucket: Long? by mutableStateOf(null)
        private set

    /** Selected day's releases with filters applied. */
    val selectedItems: List<CalendarEntry>
        get() {
            val items = cacheState[selectedDayBucket].orEmpty()
            val season = seasonFilter
            val status = statusFilter
            if (season == CalendarSeasonFilter.ALL && status == CalendarStatusFilter.ALL) return items
            val current = currentSeason()
            return items.filter { entry ->
                val item = entry.item
                season.matches(item.season, item.seasonYear, current) &&
                    status.matches(item.userStatus, item.season, item.seasonYear)
            }
        }

    val hasLoadedDays: Boolean get() = cacheState.isNotEmpty()
    val isLoading: Boolean get() = selectedDayBucket in loadingBucketsState
    val error: NetworkError? get() = errorState

    fun updateSeasonFilter(filter: CalendarSeasonFilter) {
        seasonFilter = filter
    }

    fun updateStatusFilter(filter: CalendarStatusFilter) {
        statusFilter = filter
    }

    fun selectDay(dayBucket: Long?) {
        selectedDayBucket = dayBucket
        errorState = null
        if (dayBucket != null) fetchDay(dayBucket)
    }

    /** First selectable date: yesterday at local midnight (the date-picker
     * range starts there). */
    val minDayBucket: Long
        get() = LocalDate.fromEpochDays(todayDate().toEpochDays() - 1).toBucket()

    /** Last selectable date: 150 days out (Otraku parity for the picker). */
    val maxDayBucket: Long
        get() = LocalDate.fromEpochDays(todayDate().toEpochDays() + 150).toBucket()

    /** Today at local midnight (CLEAR target of the date button). */
    val todayBucket: Long
        get() = todayDate().toBucket()

    /**
     * Moves the selection by calendar days — empty days included (Otraku
     * parity: arrows traverse dates, not releases) — clamped to
     * [minDayBucket, maxDayBucket]. Date arithmetic runs on epoch days,
     * so DST transitions (23/25h days) never drift the selection.
     */
    fun stepDay(delta: Int) {
        val current = selectedDayBucket ?: todayDate().toBucket()
        val shifted = LocalDate.fromEpochDays(
            current.toLocalDate().toEpochDays() + delta,
        ).toBucket()
        selectDay(shifted.coerceIn(minDayBucket, maxDayBucket))
    }

    /** Opens the schedule on today (cached days re-select for free). */
    fun load() {
        selectDay(selectedDayBucket ?: todayBucket)
    }

    /** Re-fetches the selected day, bypassing the cache (pull + retry). */
    fun reloadSelected() {
        val bucket = selectedDayBucket ?: todayBucket
        selectedDayBucket = bucket
        fetchDay(bucket, force = true)
    }

    private fun fetchDay(bucket: Long, force: Boolean = false) {
        if (!force && cacheState.containsKey(bucket)) return
        if (bucket in loadingBucketsState) return
        loadingBucketsState = loadingBucketsState + bucket
        errorState = null
        scope.launch {
            try {
                val zone = timeZoneProvider()
                val date = Instant.fromEpochSeconds(bucket).toLocalDateTime(zone).date
                val endExclusive = LocalDate.fromEpochDays(date.toEpochDays() + 1)
                    .atStartOfDayIn(zone).epochSeconds
                when (val result = repository.loadDay(bucket, endExclusive - 1)) {
                    is NetworkResult.Success -> {
                        cacheState = cacheState + (bucket to result.value)
                        if (selectedDayBucket == bucket) errorState = null
                    }
                    is NetworkResult.Failure -> {
                        if (selectedDayBucket == bucket) errorState = result.error
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                if (selectedDayBucket == bucket) errorState = NetworkError.Unknown(throwable)
            } finally {
                loadingBucketsState = loadingBucketsState - bucket
            }
        }
    }

    private fun todayDate(): LocalDate =
        Instant.fromEpochSeconds(nowProvider()).toLocalDateTime(timeZoneProvider()).date

    private fun Long.toLocalDate(): LocalDate =
        Instant.fromEpochSeconds(this).toLocalDateTime(timeZoneProvider()).date

    private fun LocalDate.toBucket(): Long = atStartOfDayIn(timeZoneProvider()).epochSeconds
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createCalendarStateHolder(
    repository: CalendarRepository,
    scope: CoroutineScope,
): CalendarStateHolder {
    return CalendarStateHolder(repository, scope)
}
