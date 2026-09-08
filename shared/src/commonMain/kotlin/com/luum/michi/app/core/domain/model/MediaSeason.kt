package com.luum.michi.app.core.domain.model

internal enum class MediaSeason { WINTER, SPRING, SUMMER, FALL }

internal fun parseMediaSeason(raw: String?): MediaSeason? = when (raw?.uppercase()) {
    "WINTER" -> MediaSeason.WINTER
    "SPRING" -> MediaSeason.SPRING
    "SUMMER" -> MediaSeason.SUMMER
    "FALL" -> MediaSeason.FALL
    else -> null
}

internal data class MediaSeasonYear(val season: MediaSeason, val year: Int)

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

internal fun MediaSeason.startMonth(): Int = when (this) {
    MediaSeason.WINTER -> 1
    MediaSeason.SPRING -> 4
    MediaSeason.SUMMER -> 7
    MediaSeason.FALL -> 10
}

internal fun MediaSeasonYear.startEpochSeconds(): Long =
    calendarPartsToMillis(CalendarDateParts(year, season.startMonth(), 1)) / 1000L

internal expect fun currentSeasonAndYear(): MediaSeasonYear

/**
 * Returns the ISO day-of-week for the given epoch seconds in the device's local
 * time zone. 1 = Monday, ..., 7 = Sunday.
 */
internal expect fun isoDayOfWeek(epochSeconds: Long): Int
