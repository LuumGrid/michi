package com.luum.michi.app.calendar

import com.luum.michi.app.calendar.domain.CalendarDay
import com.luum.michi.app.calendar.domain.CalendarEntry
import com.luum.michi.app.calendar.domain.CalendarFeed
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.domain.model.CalendarSeasonFilter
import com.luum.michi.app.calendar.domain.model.CalendarStatusFilter
import com.luum.michi.app.calendar.domain.model.ReleaseItem
import com.luum.michi.app.calendar.domain.model.matches
import com.luum.michi.app.calendar.repository.toCalendarFeed
import com.luum.michi.app.calendar.ui.state.CalendarStateHolder
import com.luum.michi.app.core.domain.medialist.MediaListStatus
import com.luum.michi.app.core.domain.model.MediaSeason
import com.luum.michi.app.core.domain.model.MediaSeasonYear
import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.core.repository.anilist.dto.AiringScheduleDto
import com.luum.michi.app.core.repository.anilist.dto.MediaDto
import com.luum.michi.app.core.repository.anilist.dto.MediaExternalLinkDto
import com.luum.michi.app.core.repository.anilist.dto.MediaTitleDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Current = MediaSeasonYear(MediaSeason.SPRING, 2025)

private fun item(
    season: MediaSeason? = MediaSeason.SPRING,
    seasonYear: Int? = 2025,
    userStatus: MediaListStatus? = null,
) = ReleaseItem(
    title = "T",
    release = "Ep 1",
    time = "12:00",
    paletteHex = null,
    userStatus = userStatus,
    season = season,
    seasonYear = seasonYear,
)

class CalendarFilterTest {

    @Test
    fun seasonBucketsMatchRelativeSeasons() {
        assertTrue(CalendarSeasonFilter.CURRENT.matches(MediaSeason.SPRING, 2025, Current))
        assertTrue(CalendarSeasonFilter.PREVIOUS.matches(MediaSeason.WINTER, 2025, Current))
        assertTrue(CalendarSeasonFilter.NEXT.matches(MediaSeason.SUMMER, 2025, Current))
        assertTrue(CalendarSeasonFilter.NEXT.matches(MediaSeason.WINTER, 2025, MediaSeasonYear(MediaSeason.FALL, 2024)))
        assertFalse(CalendarSeasonFilter.CURRENT.matches(MediaSeason.SPRING, 2024, Current))
        assertFalse(CalendarSeasonFilter.CURRENT.matches(MediaSeason.SUMMER, 2025, Current))
    }

    @Test
    fun seasonOtherMeansUntracked() {
        assertTrue(CalendarSeasonFilter.OTHER.matches(null, null, Current))
        assertTrue(CalendarSeasonFilter.OTHER.matches(null, 2025, Current))
        assertTrue(CalendarSeasonFilter.OTHER.matches(MediaSeason.SPRING, null, Current))
        assertFalse(CalendarSeasonFilter.OTHER.matches(MediaSeason.SPRING, 2025, Current))
        assertTrue(CalendarSeasonFilter.ALL.matches(null, null, Current))
    }

    @Test
    fun statusIsStrictWatchingPlanning() {
        assertTrue(CalendarStatusFilter.WATCHING_PLANNING.matches(MediaListStatus.CURRENT, MediaSeason.SPRING, 2025))
        assertTrue(CalendarStatusFilter.WATCHING_PLANNING.matches(MediaListStatus.PLANNING, null, null))
        assertFalse(CalendarStatusFilter.WATCHING_PLANNING.matches(MediaListStatus.PAUSED, MediaSeason.SPRING, 2025))
        assertFalse(CalendarStatusFilter.WATCHING_PLANNING.matches(null, MediaSeason.SPRING, 2025))
    }

    @Test
    fun statusOtherCornerOverlapsHonestly() {
        // No season + no list: answers both questions per the literal predicates.
        assertTrue(CalendarStatusFilter.NOT_IN_LIST.matches(null, null, null))
        assertTrue(CalendarStatusFilter.OTHER.matches(null, null, null))
        assertTrue(CalendarStatusFilter.NOT_IN_LIST.matches(null, MediaSeason.SPRING, 2025))
        assertFalse(CalendarStatusFilter.OTHER.matches(null, MediaSeason.SPRING, 2025))
        assertTrue(CalendarStatusFilter.ALL.matches(MediaListStatus.CURRENT, MediaSeason.SPRING, 2025))
    }

    @Test
    fun overlappingLoadIssuesASingleFetch() {
        val gate = CompletableDeferred<Unit>()
        var fetches = 0
        val repository = object : CalendarRepository {
            override fun loadFeed(): Flow<NetworkResult<CalendarFeed>> = flow {
                fetches++
                gate.await()
                emit(NetworkResult.Success(CalendarFeed(emptyList())))
            }
        }
        val holder = CalendarStateHolder(repository, CoroutineScope(Dispatchers.Unconfined))
        holder.load()
        holder.load()
        gate.complete(Unit)
        assertEquals(1, fetches)
    }

    private fun holderWith(vararg days: CalendarDay): CalendarStateHolder {
        val repository = object : CalendarRepository {
            override fun loadFeed(): Flow<NetworkResult<CalendarFeed>> =
                flowOf(NetworkResult.Success(CalendarFeed(days.toList())))
        }
        val holder = CalendarStateHolder(repository, CoroutineScope(Dispatchers.Unconfined))
        holder.load()
        return holder
    }

    private fun day(bucket: Long, offset: Int, vararg items: ReleaseItem) = CalendarDay(
        dayBucket = bucket,
        isoDayOfWeek = 1,
        offsetFromToday = offset,
        day = 1,
        month = 1,
        year = 2025,
        items = items.mapIndexed { index, item -> CalendarEntry(scheduleId = index, item = item) },
    )

    private val Santiago = TimeZone.of("America/Santiago")

    private fun epoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime(year, month, day, hour, minute).toInstant(Santiago).epochSeconds

    private fun schedule(
        id: Int,
        airingAt: Long,
        episode: Int = 1,
        media: MediaDto? = MediaDto(id = id),
    ) = AiringScheduleDto(
        id = id,
        airingAt = airingAt,
        episode = episode,
        media = media,
    )

    @Test
    fun springForwardWeekendOffsetsStayExact() {
        // Chile springs forward on 2025-09-07 00:00 (-> 01:00): a 23h day.
        // Epoch division would report offset 0 for Sep 7 (82800/86400 truncates).
        val feed = listOf(
            schedule(1, epoch(2025, 9, 6, 20, 0)),
            schedule(2, epoch(2025, 9, 7, 10, 0)),
        ).toCalendarFeed(epoch(2025, 9, 6, 15, 0)) { Santiago }
        assertEquals(2, feed.days.size)
        assertEquals(0, feed.days[0].offsetFromToday)
        assertEquals(1, feed.days[1].offsetFromToday)
        assertEquals(1, feed.days[0].items.size)
        assertEquals(1, feed.days[1].items.size)
    }

    @Test
    fun fallBackRepeatedHourStaysInOneBucket() {
        // Chile falls back on 2026-04-05 00:00: 00:00-01:00 happens twice (25h day).
        val feed = listOf(
            schedule(1, epoch(2026, 4, 5, 0, 30)),
            schedule(2, epoch(2026, 4, 5, 12, 0)),
            schedule(3, epoch(2026, 4, 6, 10, 0)),
        ).toCalendarFeed(epoch(2026, 4, 4, 12, 0)) { Santiago }
        assertEquals(2, feed.days.size)
        assertEquals(2, feed.days[0].items.size)
        assertEquals(1, feed.days[0].offsetFromToday)
        assertEquals(2, feed.days[1].offsetFromToday)
    }

    @Test
    fun sameDayGroupsIntoOneBucketPreservingIds() {
        val feed = listOf(
            schedule(1, epoch(2025, 9, 6, 20, 0)),
            schedule(2, epoch(2025, 9, 6, 22, 0)),
        ).toCalendarFeed(epoch(2025, 9, 6, 15, 0)) { Santiago }
        assertEquals(1, feed.days.size)
        assertEquals(listOf(1, 2), feed.days.single().items.map { it.scheduleId })
    }

    @Test
    fun nullMediaSchedulesAreSkipped() {
        val feed = listOf(
            schedule(1, epoch(2025, 9, 6, 20, 0), media = null),
            schedule(2, epoch(2025, 9, 6, 22, 0)),
        ).toCalendarFeed(epoch(2025, 9, 6, 15, 0)) { Santiago }
        assertEquals(1, feed.days.size)
        assertEquals(listOf(2), feed.days.single().items.map { it.scheduleId })
    }

    @Test
    fun releaseLabelUsesTotalWhenKnown() {
        fun label(episodes: Int?): String {
            val feed = listOf(
                schedule(
                    1,
                    epoch(2025, 9, 6, 20, 0),
                    episode = 5,
                    media = MediaDto(id = 1, episodes = episodes),
                ),
            ).toCalendarFeed(epoch(2025, 9, 6, 15, 0)) { Santiago }
            return feed.days.single().items.single().item.release
        }
        assertEquals("Ep 5 / 12", label(12))
        assertEquals("Ep 5", label(null))
        assertEquals("Ep 5", label(0))
    }

    @Test
    fun airingTimeIsZeroPadded() {
        val feed = listOf(
            schedule(1, epoch(2025, 9, 6, 9, 5)),
        ).toCalendarFeed(epoch(2025, 9, 6, 8, 0)) { Santiago }
        // Wall time follows the device zone by design; only the shape is pinned.
        assertTrue(Regex("\\d{2}:\\d{2}").matches(feed.days.single().items.single().item.time))
    }

    @Test
    fun titleFallsBackThroughChain() {
        fun title(title: MediaTitleDto?): String {
            val feed = listOf(
                schedule(1, epoch(2025, 9, 6, 20, 0), media = MediaDto(id = 1, title = title)),
            ).toCalendarFeed(epoch(2025, 9, 6, 15, 0)) { Santiago }
            return feed.days.single().items.single().item.title
        }
        assertEquals("Preferred", title(MediaTitleDto(userPreferred = "Preferred", english = "EN")))
        assertEquals("Nativo", title(MediaTitleDto(native = "Nativo")))
        assertEquals("", title(null))
    }

    @Test
    fun streamingPlatformsKeepOnlyEnabledStreamingWithUrl() {
        fun link(site: String?, url: String?, type: String?, disabled: Boolean?) =
            MediaExternalLinkDto(site = site, url = url, type = type, isDisabled = disabled)
        val feed = listOf(
            schedule(
                1,
                epoch(2025, 9, 6, 20, 0),
                media = MediaDto(
                    id = 1,
                    externalLinks = listOf(
                        link("Crunchyroll", "https://x/1", "STREAMING", null),
                        link("Crunchyroll", "https://x/2", "STREAMING", null),
                        link("Dead", "https://x/3", "STREAMING", true),
                        link("Info", "https://x/4", "INFO", null),
                        link("", "https://x/5", "STREAMING", null),
                        link("NoUrl", "", "STREAMING", null),
                    ),
                ),
            ),
        ).toCalendarFeed(epoch(2025, 9, 6, 15, 0)) { Santiago }
        assertEquals(
            listOf("Crunchyroll"),
            feed.days.single().items.single().item.streamingPlatforms.map { it.site },
        )
    }

    @Test
    fun stepDayMovesOnlyWithinVisibleDays() {
        val holder = holderWith(
            day(10, offset = -1, item(season = MediaSeason.SPRING, seasonYear = 2025)),
            day(20, offset = 0, item(season = null, seasonYear = null)),
            day(30, offset = 1, item(season = null, seasonYear = null)),
        )
        holder.updateSeasonFilter(CalendarSeasonFilter.OTHER)
        assertEquals(20, holder.selectedDayBucket)
        holder.stepDay(1)
        assertEquals(30, holder.selectedDayBucket)
        holder.stepDay(1)
        assertEquals(30, holder.selectedDayBucket)
        holder.stepDay(-5)
        assertEquals(20, holder.selectedDayBucket)
    }

    @Test
    fun stepDayClampsToLoadedRange() {        val holder = holderWith(
            day(10, offset = -1, item()),
            day(20, offset = 0, item()),
            day(30, offset = 1, item()),
        )
        assertEquals(20, holder.selectedDayBucket)
        holder.stepDay(1)
        assertEquals(30, holder.selectedDayBucket)
        holder.stepDay(1)
        assertEquals(30, holder.selectedDayBucket)
        holder.stepDay(-5)
        assertEquals(10, holder.selectedDayBucket)
    }

    @Test
    fun visibleDaysDropsDaysEmptiedByFiltering() {
        val holder = holderWith(
            day(10, offset = 0, item(season = MediaSeason.SPRING, seasonYear = 2025)),
            day(20, offset = 1, item(season = null, seasonYear = null)),
        )
        assertEquals(2, holder.visibleDays.size)
        holder.updateSeasonFilter(CalendarSeasonFilter.OTHER)
        assertEquals(listOf(20L), holder.visibleDays.map { it.dayBucket })
        holder.updateSeasonFilter(CalendarSeasonFilter.ALL)
        holder.updateStatusFilter(CalendarStatusFilter.WATCHING_PLANNING)
        assertEquals(0, holder.visibleDays.size)
    }
}
