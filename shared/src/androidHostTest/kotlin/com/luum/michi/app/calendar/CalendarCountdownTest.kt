package com.luum.michi.app.calendar

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import com.luum.michi.app.core.media.domain.airingCountdown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalendarCountdownTest {

    @Test
    fun futureAiringShowsHoursAndMinutes() {
        assertEquals(
            "in 3 hours 40 minutes",
            airingCountdown(EnglishStrings, 3 * 3600L + 40 * 60),
        )
        assertEquals(
            "en 3 horas 40 minutos",
            airingCountdown(SpanishStrings, 3 * 3600L + 40 * 60),
        )
    }

    @Test
    fun daysAwayShowsDaysAndHours() {
        val inFourDays = 4 * 86_400L + 7 * 3600 + 30 * 60
        assertEquals("in 4 days 7 hours", airingCountdown(EnglishStrings, inFourDays))
        assertEquals("en 4 días 7 horas", airingCountdown(SpanishStrings, inFourDays))
    }

    @Test
    fun singularUnitsAndMinutesOnly() {
        assertEquals("in 1 hour 1 minute", airingCountdown(EnglishStrings, 3600L + 60))
        assertEquals("en 1 hora 1 minuto", airingCountdown(SpanishStrings, 3600L + 60))
        assertEquals("in 5 minutes", airingCountdown(EnglishStrings, 5 * 60L))
        assertEquals("en 5 minutos", airingCountdown(SpanishStrings, 5 * 60L))
    }

    @Test
    fun zeroAndPastAiringHideCountdown() {
        assertNull(airingCountdown(EnglishStrings, 0L))
        assertNull(airingCountdown(EnglishStrings, -60L))
        assertNull(airingCountdown(SpanishStrings, -3600L))
    }
}
