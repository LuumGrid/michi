package com.luum.michi.app.core.domain.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

internal data class CalendarDateParts(
    val year: Int,
    val month: Int,
    val day: Int,
)

/** Implemented with kotlinx-datetime (always ISO): identical on every platform,
 *  immune to non-Gregorian locale calendars. */
internal fun millisToCalendarParts(millis: Long): CalendarDateParts {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
    return CalendarDateParts(year = date.year, month = date.month.number, day = date.day)
}

internal fun calendarPartsToMillis(parts: CalendarDateParts): Long =
    LocalDateTime(parts.year, parts.month, parts.day, 0, 0)
        .toInstant(TimeZone.UTC)
        .toEpochMilliseconds()

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

internal fun localMidnightEpoch(
    epochSeconds: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Long {
    val date = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(timeZone).date
    return date.atStartOfDayIn(timeZone).epochSeconds
}
