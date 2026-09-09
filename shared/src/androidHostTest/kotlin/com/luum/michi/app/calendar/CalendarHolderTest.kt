package com.luum.michi.app.calendar

import com.luum.michi.app.calendar.domain.CalendarDay
import com.luum.michi.app.calendar.domain.CalendarEntry
import com.luum.michi.app.calendar.domain.CalendarFeed
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.domain.model.ReleaseItem
import com.luum.michi.app.calendar.ui.state.CalendarStateHolder
import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun release(title: String = "T") = ReleaseItem(
    title = title,
    release = "Ep 1",
    time = "12:00",
    paletteHex = null,
)

private fun day(bucket: Long, offset: Int, vararg titles: String) = CalendarDay(
    dayBucket = bucket,
    isoDayOfWeek = 1,
    offsetFromToday = offset,
    day = 1,
    month = 1,
    year = 2025,
    items = titles.mapIndexed { index, title ->
        CalendarEntry(scheduleId = index, item = release(title))
    },
)

private class FakeCalendarRepository(
    var feed: CalendarFeed = CalendarFeed(emptyList()),
    var failNext: Boolean = false,
) : CalendarRepository {
    override fun loadFeed(): Flow<NetworkResult<CalendarFeed>> = flowOf(
        if (failNext) {
            failNext = false
            NetworkResult.Failure(NetworkError.Http(500, null))
        } else {
            NetworkResult.Success(feed)
        },
    )
}

private fun holderWith(
    feed: CalendarFeed = CalendarFeed(emptyList()),
    failure: NetworkError? = null,
): CalendarStateHolder {
    val holder = CalendarStateHolder(
        FakeCalendarRepository(feed, failure != null),
        CoroutineScope(Dispatchers.Unconfined),
    )
    holder.load()
    return holder
}

class CalendarHolderTest {

    @Test
    fun successSelectsTodayBucket() {
        val holder = holderWith(
            CalendarFeed(
                listOf(
                    day(10, offset = -1, "A"),
                    day(20, offset = 0, "B"),
                    day(30, offset = 1, "C"),
                ),
            ),
        )
        assertEquals(20, holder.selectedDayBucket)
        assertNull(holder.error)
        assertEquals(listOf("A", "B", "C"), holder.days.flatMap { day -> day.items.map { it.item.title } })
    }

    @Test
    fun withoutTodaySelectsFirstDay() {
        val holder = holderWith(
            CalendarFeed(listOf(day(30, offset = 1, "C"), day(40, offset = 2, "D"))),
        )
        assertEquals(30, holder.selectedDayBucket)
    }

    @Test
    fun failureOnEmptyDaysSetsError() {
        val holder = holderWith(failure = NetworkError.Http(500, null))
        assertTrue(holder.days.isEmpty())
        assertTrue(holder.error is NetworkError.Http)
    }

    @Test
    fun failureWithDataKeepsDaysAndLeavesErrorClear() {
        val repository = FakeCalendarRepository(
            CalendarFeed(listOf(day(10, offset = 0, "A"))),
        )
        val holder = CalendarStateHolder(repository, CoroutineScope(Dispatchers.Unconfined))
        holder.load()
        repository.failNext = true
        holder.load()
        assertEquals(1, holder.days.size)
        assertNull(holder.error)
    }

    @Test
    fun selectDaySetsBucket() {
        val holder = holderWith(CalendarFeed(listOf(day(10, offset = 0, "A"))))
        holder.selectDay(10)
        assertEquals(10, holder.selectedDayBucket)
        holder.selectDay(null)
        assertNull(holder.selectedDayBucket)
    }
}
