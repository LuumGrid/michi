package com.luum.michi.app.core.media.domain

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

data class MediaReleaseDateTime(
    val day: Int,
    val month: Int,
    val year: Int,
    val hour: Int,
    val minute: Int,
) {
    fun formatReadableDateTime(monthName: (Int) -> String): String {
        return "$day ${monthName(month)} $year, ${hour.twoDigits()}:${minute.twoDigits()}"
    }
}

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')

internal fun Long.toLocalMediaReleaseDateTime(): MediaReleaseDateTime {
    val dateTime = Instant.fromEpochSeconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    return MediaReleaseDateTime(
        day = dateTime.day,
        month = dateTime.month.number,
        year = dateTime.year,
        hour = dateTime.hour,
        minute = dateTime.minute,
    )
}
