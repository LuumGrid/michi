package com.luum.michi.app.core.media

internal enum class MediaSeason { WINTER, SPRING, SUMMER, FALL }

internal data class MediaSeasonYear(val season: MediaSeason, val year: Int)

internal fun MediaSeasonYear.next(): MediaSeasonYear = when (season) {
    MediaSeason.WINTER -> MediaSeasonYear(MediaSeason.SPRING, year)
    MediaSeason.SPRING -> MediaSeasonYear(MediaSeason.SUMMER, year)
    MediaSeason.SUMMER -> MediaSeasonYear(MediaSeason.FALL, year)
    MediaSeason.FALL -> MediaSeasonYear(MediaSeason.WINTER, year + 1)
}

internal fun seasonForMonth(month: Int): MediaSeason = when (month) {
    1, 2, 3 -> MediaSeason.WINTER
    4, 5, 6 -> MediaSeason.SPRING
    7, 8, 9 -> MediaSeason.SUMMER
    else -> MediaSeason.FALL
}

internal expect fun currentSeasonAndYear(): MediaSeasonYear

/**
 * Returns the ISO day-of-week for the given epoch seconds in the device's local
 * time zone. 1 = Monday, ..., 7 = Sunday.
 */
internal expect fun isoDayOfWeek(epochSeconds: Long): Int
