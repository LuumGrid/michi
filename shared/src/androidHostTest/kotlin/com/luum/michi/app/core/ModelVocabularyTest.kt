package com.luum.michi.app.core

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.model.CalendarDateParts
import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.model.MediaSeasonYear
import com.luum.michi.app.core.model.MediaWorkStatus
import com.luum.michi.app.core.model.calendarPartsToMillis
import com.luum.michi.app.core.model.label
import com.luum.michi.app.core.model.millisToCalendarParts
import com.luum.michi.app.core.model.next
import com.luum.michi.app.core.model.parseMediaFormat
import com.luum.michi.app.core.model.parseMediaWorkStatus
import com.luum.michi.app.core.model.previous
import com.luum.michi.app.core.model.seasonForMonth
import com.luum.michi.app.core.model.startEpochSeconds
import com.luum.michi.app.core.model.toTitleCase
import com.luum.michi.app.core.model.utcMillisToLocalMidnightBucket
import com.luum.michi.app.core.navigation.domain.isWebUrl
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelVocabularyTest {

    @Test
    fun formatParsesCaseInsensitively() {
        assertEquals(MediaFormat.TV, parseMediaFormat("tv"))
        assertEquals(MediaFormat.ONE_SHOT, parseMediaFormat("ONE_SHOT"))
        assertEquals(MediaFormat.UNKNOWN, parseMediaFormat("BOGUS"))
        assertNull(parseMediaFormat(null)?.takeIf { it != MediaFormat.UNKNOWN })
    }

    @Test
    fun formatLabelKeepsAcronymsUppercase() {
        assertEquals("TV Short", MediaFormat.TV_SHORT.label())
        assertEquals("TV", MediaFormat.TV.label())
        assertEquals("OVA", MediaFormat.OVA.label())
        assertEquals("ONA", MediaFormat.ONA.label())
        assertEquals("Movie", MediaFormat.MOVIE.label())
        assertEquals("One Shot", MediaFormat.ONE_SHOT.label())
        assertEquals("Unknown", MediaFormat.UNKNOWN.label())
        assertEquals("Manga", MediaFormat.UNKNOWN.label(unknownFallback = "Manga"))
    }

    @Test
    fun titleCaseKeepsAcronymsUppercase() {
        assertEquals("TV Short", "TV_SHORT".toTitleCase())
        assertEquals("Side Story", "SIDE_STORY".toTitleCase())
    }

    @Test
    fun seasonForMonthBoundaries() {
        assertEquals(MediaSeason.WINTER, seasonForMonth(1))
        assertEquals(MediaSeason.WINTER, seasonForMonth(3))
        assertEquals(MediaSeason.SPRING, seasonForMonth(4))
        assertEquals(MediaSeason.SPRING, seasonForMonth(6))
        assertEquals(MediaSeason.SUMMER, seasonForMonth(7))
        assertEquals(MediaSeason.SUMMER, seasonForMonth(9))
        assertEquals(MediaSeason.FALL, seasonForMonth(10))
        assertEquals(MediaSeason.FALL, seasonForMonth(12))
    }

    @Test
    fun seasonStepRolloverYear() {
        assertEquals(
            MediaSeasonYear(MediaSeason.WINTER, 2025),
            MediaSeasonYear(MediaSeason.FALL, 2024).next(),
        )
        assertEquals(
            MediaSeasonYear(MediaSeason.FALL, 2024),
            MediaSeasonYear(MediaSeason.WINTER, 2025).previous(),
        )
        assertEquals(
            MediaSeasonYear(MediaSeason.SUMMER, 2025),
            MediaSeasonYear(MediaSeason.SPRING, 2025).next(),
        )
    }

    @Test
    fun seasonStartIsFirstOfStartMonthUtc() {
        assertEquals(1735689600L, MediaSeasonYear(MediaSeason.WINTER, 2025).startEpochSeconds())
    }

    @Test
    fun millisPartsRoundtrip() {
        assertEquals(CalendarDateParts(1970, 1, 1), millisToCalendarParts(0L))
        val parts = CalendarDateParts(2025, 3, 15)
        assertEquals(parts, millisToCalendarParts(calendarPartsToMillis(parts)))
    }

    @Test
    fun workStatusParsesCaseInsensitively() {
        assertEquals(MediaWorkStatus.FINISHED, parseMediaWorkStatus("finished"))
        assertEquals(MediaWorkStatus.RELEASING, parseMediaWorkStatus("RELEASING"))
        assertEquals(MediaWorkStatus.NOT_YET_RELEASED, parseMediaWorkStatus("not_yet_released"))
        assertNull(parseMediaWorkStatus("BOGUS"))
        assertNull(parseMediaWorkStatus(null))
    }

    @Test
    fun workStatusReleasingSplitsByType() {
        assertEquals("Airing", MediaWorkStatus.RELEASING.label(EnglishStrings, isManga = false))
        assertEquals("Publishing", MediaWorkStatus.RELEASING.label(EnglishStrings, isManga = true))
        assertEquals("Finished", MediaWorkStatus.FINISHED.label(EnglishStrings, isManga = false))
    }

    @Test
    fun seasonLabelsTranslate() {
        assertEquals("Winter", MediaSeason.WINTER.label(EnglishStrings))
        assertEquals("Fall", MediaSeason.FALL.label(EnglishStrings))
    }

    @Test
    fun utcPickerMillisMapToLocalMidnightBucket() {
        // Oct 30 2026 00:00 UTC picked in Mexico City (UTC-6, no DST) is
        // still Oct 29 locally — reading in the local zone shifts a day
        // behind, so the helper reads the calendar date in UTC.
        val zone = TimeZone.of("America/Mexico_City")
        assertEquals(1793340000L, utcMillisToLocalMidnightBucket(1793318400000L, zone))
    }

    @Test
    fun webUrlGuardAcceptsHttpOnly() {
        assertEquals(true, isWebUrl("https://example.com/x"))
        assertEquals(true, isWebUrl("HTTP://example.com"))
        assertEquals(false, isWebUrl("javascript:alert(1)"))
        assertEquals(false, isWebUrl("michi://oauth/callback"))
        assertEquals(false, isWebUrl(null))
        assertEquals(false, isWebUrl(""))
    }
}
