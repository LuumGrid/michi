package com.luum.michi.app.core.media.domain

import com.luum.michi.app.core.language.domain.LanguageStrings
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

internal enum class MediaSeason { WINTER, SPRING, SUMMER, FALL }

internal fun parseMediaSeason(raw: String?): MediaSeason? = when (raw?.uppercase()) {
    "WINTER" -> MediaSeason.WINTER
    "SPRING" -> MediaSeason.SPRING
    "SUMMER" -> MediaSeason.SUMMER
    "FALL" -> MediaSeason.FALL
    else -> null
}

/** Shared season label (mediaDetail had a private twin — scopes must not
 *  import each other, so the neutral one lives here). */
internal fun MediaSeason.label(strings: LanguageStrings): String = when (this) {
    MediaSeason.WINTER -> strings.seasonWinterLabel
    MediaSeason.SPRING -> strings.seasonSpringLabel
    MediaSeason.SUMMER -> strings.seasonSummerLabel
    MediaSeason.FALL -> strings.seasonFallLabel
}

internal data class MediaSeasonYear(val season: MediaSeason, val year: Int)

/** Localized rail/toolbar title: e.g. "Fall 2026". */
internal fun MediaSeasonYear.label(strings: LanguageStrings): String =
    "${season.label(strings)} $year"

internal fun MediaSeasonYear.next(): MediaSeasonYear = when (season) {
    MediaSeason.WINTER -> MediaSeasonYear(MediaSeason.SPRING, year)
    MediaSeason.SPRING -> MediaSeasonYear(MediaSeason.SUMMER, year)
    MediaSeason.SUMMER -> MediaSeasonYear(MediaSeason.FALL, year)
    MediaSeason.FALL -> MediaSeasonYear(MediaSeason.WINTER, year + 1)
}

internal fun MediaSeasonYear.previous(): MediaSeasonYear = when (season) {
    MediaSeason.WINTER -> MediaSeasonYear(MediaSeason.FALL, year - 1)
    MediaSeason.SPRING -> MediaSeasonYear(MediaSeason.WINTER, year)
    MediaSeason.SUMMER -> MediaSeasonYear(MediaSeason.SPRING, year)
    MediaSeason.FALL -> MediaSeasonYear(MediaSeason.SUMMER, year)
}

internal fun seasonForMonth(month: Int): MediaSeason = when (month) {
    1, 2, 3 -> MediaSeason.WINTER
    4, 5, 6 -> MediaSeason.SPRING
    7, 8, 9 -> MediaSeason.SUMMER
    else -> MediaSeason.FALL
}

/** Implemented with kotlinx-datetime (always ISO): identical on every platform. */
internal fun currentSeasonAndYear(): MediaSeasonYear {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return MediaSeasonYear(season = seasonForMonth(today.month.number), year = today.year)
}
