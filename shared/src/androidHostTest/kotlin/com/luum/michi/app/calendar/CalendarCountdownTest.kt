package com.luum.michi.app.calendar

import com.luum.michi.app.calendar.domain.model.airingCountdownLabel
import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalendarCountdownTest {

    @Test
    fun futureAiringShowsHoursAndMinutes() {
        val now = 1_000_000L
        assertEquals(
            "in 3 hours 40 minutes",
            airingCountdownLabel(EnglishStrings, now + 3 * 3600 + 40 * 60, now),
        )
        assertEquals(
            "en 3 horas 40 minutos",
            airingCountdownLabel(SpanishStrings, now + 3 * 3600 + 40 * 60, now),
        )
    }

    @Test
    fun singularUnitsAndMinutesOnly() {
        val now = 1_000_000L
        assertEquals(
            "in 1 hour 1 minute",
            airingCountdownLabel(EnglishStrings, now + 3600 + 60, now),
        )
        assertEquals(
            "en 1 hora 1 minuto",
            airingCountdownLabel(SpanishStrings, now + 3600 + 60, now),
        )
        assertEquals(
            "in 5 minutes",
            airingCountdownLabel(EnglishStrings, now + 5 * 60, now),
        )
        assertEquals(
            "en 5 minutos",
            airingCountdownLabel(SpanishStrings, now + 5 * 60, now),
        )
    }

    @Test
    fun pastAiringHidesCountdown() {
        val now = 1_000_000L
        assertNull(airingCountdownLabel(EnglishStrings, now - 60, now))
        assertNull(airingCountdownLabel(EnglishStrings, now, now))
        assertNull(airingCountdownLabel(SpanishStrings, now - 3600, now))
    }
}
