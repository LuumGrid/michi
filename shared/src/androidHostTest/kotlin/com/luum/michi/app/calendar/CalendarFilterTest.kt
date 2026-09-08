package com.luum.michi.app.calendar

import com.luum.michi.app.calendar.domain.CalendarDay
import com.luum.michi.app.calendar.domain.CalendarEntry
import com.luum.michi.app.calendar.domain.CalendarFeed
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.domain.model.CalendarSeasonFilter
import com.luum.michi.app.calendar.domain.model.CalendarStatusFilter
import com.luum.michi.app.calendar.domain.model.ReleaseItem
import com.luum.michi.app.calendar.domain.model.matches
import com.luum.michi.app.calendar.ui.state.CalendarStateHolder
import com.luum.michi.app.core.domain.medialist.MediaListStatus
import com.luum.michi.app.core.domain.model.MediaSeason
import com.luum.michi.app.core.domain.model.MediaSeasonYear
import com.luum.michi.app.core.domain.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    @Test
    fun stepDayClampsToLoadedRange() {
        val holder = holderWith(
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
