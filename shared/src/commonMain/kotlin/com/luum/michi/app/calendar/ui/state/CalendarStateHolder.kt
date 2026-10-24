package com.luum.michi.app.calendar.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.time.TimeSource
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
import com.luum.michi.app.core.media.domain.MediaSeasonYear
import com.luum.michi.app.core.media.domain.currentSeasonAndYear
import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
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

    // When each bucket was fetched, so the cache can expire with the shared TTL
    // instead of serving a day forever.
    private val timeMark = TimeSource.Monotonic
    private val loadedAt = mutableMapOf<Long, TimeSource.Monotonic.ValueTimeMark>()
    // Bumped when the cache is invalidated: a fetch launched before it must not
    // write its (now stale) result.
    private var invalidation = 0
    // The invalidation each in-flight bucket was launched under, so a fetch that
    // predates the last invalidation can be superseded instead of skipped.
    private val loadingInvalidation = mutableMapOf<Long, Int>()

    // Ticking "now" for the countdown line: refreshed every minute while the
    // calendar is visible (startTicking/stopTicking), never read from the clock
    // inside composition.
    var nowEpochSeconds: Long by mutableStateOf(nowProvider())
        private set
    private var tickerJob: Job? = null

    /** Zone the row times are formatted in (same one the day buckets use). */
    val timeZone: TimeZone get() = timeZoneProvider()

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
    /** True once the selected day's slice has been fetched (even if empty), so a
     *  failed refresh of a visible day is a compact error, not a blocking one. */
    val hasLoadedSelectedDay: Boolean get() = cacheState.containsKey(selectedDayBucket)
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
        if (!force && isFresh(bucket)) return
        // Skip only an in-flight fetch launched under the current invalidation:
        // one from before the last invalidation is superseded (it discards
        // itself via the counter), so launch a fresh one.
        if (bucket in loadingBucketsState && loadingInvalidation[bucket] == invalidation) return
        val launchInvalidation = invalidation
        loadingInvalidation[bucket] = launchInvalidation
        loadingBucketsState = loadingBucketsState + bucket
        errorState = null
        scope.launch {
            try {
                val zone = timeZoneProvider()
                val date = Instant.fromEpochSeconds(bucket).toLocalDateTime(zone).date
                val endExclusive = LocalDate.fromEpochDays(date.toEpochDays() + 1)
                    .atStartOfDayIn(zone).epochSeconds
                // airingAt_greater / airingAt_lesser are exclusive: start one
                // second before local midnight so a release exactly at midnight
                // is not dropped.
                when (val result = repository.loadDay(bucket - 1, endExclusive)) {
                    is NetworkResult.Success -> {
                        // Drop a response that landed after an invalidation.
                        if (launchInvalidation == invalidation) {
                            cacheState = cacheState + (bucket to result.value)
                            loadedAt[bucket] = timeMark.markNow()
                        }
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
                // Only the newest fetch for this bucket frees it.
                if (loadingInvalidation[bucket] == launchInvalidation) {
                    loadingInvalidation.remove(bucket)
                    loadingBucketsState = loadingBucketsState - bucket
                }
            }
        }
    }

    /** A cached bucket still within the shared TTL; older slices re-fetch. */
    private fun isFresh(bucket: Long): Boolean {
        if (!cacheState.containsKey(bucket)) return false
        val mark = loadedAt[bucket] ?: return false
        return mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL
    }

    /**
     * Marks every cached day stale (a list entry changed, or the viewer/language
     * did) without dropping it: the calendar keeps showing what it had instead
     * of going blank, and the next load refetches. The counter discards an
     * in-flight fetch that started before this invalidation.
     */
    fun invalidateEntries() {
        loadedAt.clear()
        invalidation++
    }

    /** Starts the per-minute countdown tick; call while the calendar is visible. */
    fun startTicking() {
        if (tickerJob?.isActive == true) return
        nowEpochSeconds = nowProvider()
        tickerJob = scope.launch {
            while (true) {
                delay(60_000)
                nowEpochSeconds = nowProvider()
            }
        }
    }

    /** Stops the countdown tick (the calendar was closed). */
    fun stopTicking() {
        tickerJob?.cancel()
        tickerJob = null
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
