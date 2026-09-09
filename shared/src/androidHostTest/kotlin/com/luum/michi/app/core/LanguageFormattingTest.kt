package com.luum.michi.app.core

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.language.domain.SpanishStrings
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.language.domain.getLanguageStrings
import com.luum.michi.app.core.model.MediaReleaseDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val bothLanguages = listOf(EnglishStrings, SpanishStrings)

class LanguageFormattingTest {

    @Test
    fun behindLabelsUseSingularForOne() {
        assertEquals("1 episode behind", EnglishStrings.episodesBehind(1))
        assertEquals("5 episodes behind", EnglishStrings.episodesBehind(5))
        assertEquals("1 chapter behind", EnglishStrings.chaptersBehind(1))
        assertEquals("1 capitulo pendiente", SpanishStrings.chaptersBehind(1))
        assertEquals("5 capitulos pendientes", SpanishStrings.chaptersBehind(5))
    }

    @Test
    fun calendarHeaderComposesParts() {
        assertEquals("On 5 Mar 2025", EnglishStrings.calendarHeaderLabel("On", 5, 3, 2025))
        assertTrue(SpanishStrings.calendarHeaderLabel("El", 5, 3, 2025).contains("mar"))
    }

    @Test
    fun joinedLabelLocalizesMonth() {
        assertEquals("Joined Mar 2024", EnglishStrings.accountJoinedLabel(3, 2024))
        assertEquals("Se unió en mar 2024", SpanishStrings.accountJoinedLabel(3, 2024))
    }

    @Test
    fun languageFallbackDefaultsToEnglish() {
        assertTrue(getLanguageStrings(AppLanguage(code = "xx", displayName = "?")) is EnglishStrings)
        assertTrue(getLanguageStrings(AppLanguage(code = "es", displayName = "?")) is SpanishStrings)
    }

    @Test
    fun countLabels() {
        assertEquals("12 ep", EnglishStrings.exploreEpisodeCountLabel(12))
        assertEquals("5 cap", SpanishStrings.exploreChapterCountLabel(5))
    }

    @Test
    fun releaseDateLabelsFormat() {
        val dateTime = MediaReleaseDateTime(day = 5, month = 3, year = 2025, hour = 9, minute = 5)
        assertEquals("Ep. 12 5 Mar 2025, 09:05", EnglishStrings.nextEpisodeReleaseLabel(12, dateTime))
        assertEquals("5 Mar 2025, 09:05", EnglishStrings.notificationDateLabel(dateTime))
    }
}
