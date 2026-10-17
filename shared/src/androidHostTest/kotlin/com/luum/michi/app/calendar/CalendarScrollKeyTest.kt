package com.luum.michi.app.calendar

import com.luum.michi.app.calendar.domain.model.CalendarSeasonFilter
import com.luum.michi.app.calendar.domain.model.CalendarStatusFilter
import com.luum.michi.app.calendar.ui.calendarResetKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Scroll reset key: changing the day or either calendar filter returns the list
 * to the top.
 */
class CalendarScrollKeyTest {

    @Test
    fun theKeyTracksTheDayAndBothFilters() {
        val base = calendarResetKey(100L, CalendarSeasonFilter.ALL, CalendarStatusFilter.ALL)

        assertEquals(base, calendarResetKey(100L, CalendarSeasonFilter.ALL, CalendarStatusFilter.ALL))

        assertNotEquals(base, calendarResetKey(101L, CalendarSeasonFilter.ALL, CalendarStatusFilter.ALL))
        assertNotEquals(base, calendarResetKey(100L, CalendarSeasonFilter.CURRENT, CalendarStatusFilter.ALL))
        assertNotEquals(
            base,
            calendarResetKey(100L, CalendarSeasonFilter.ALL, CalendarStatusFilter.WATCHING_PLANNING),
        )
    }
}
