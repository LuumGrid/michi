package com.luum.michi.app.core.media

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal actual fun currentSeasonAndYear(): MediaSeasonYear {
    val now = LocalDate.now()
    return MediaSeasonYear(seasonForMonth(now.monthValue), now.year)
}

internal actual fun isoDayOfWeek(epochSeconds: Long): Int =
    Instant.ofEpochSecond(epochSeconds)
        .atZone(ZoneId.systemDefault())
        .dayOfWeek
        .value
