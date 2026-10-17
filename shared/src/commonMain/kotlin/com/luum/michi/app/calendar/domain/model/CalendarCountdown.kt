package com.luum.michi.app.calendar.domain.model

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * `HH:mm` of [airingAtEpochSeconds] in [timeZone] (the holder's zone, not a
 * fresh system lookup), so the row time and the day buckets agree.
 */
internal fun formatAiringTime(airingAtEpochSeconds: Long, timeZone: TimeZone): String {
    val dateTime = Instant.fromEpochSeconds(airingAtEpochSeconds).toLocalDateTime(timeZone)
    return "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
}
