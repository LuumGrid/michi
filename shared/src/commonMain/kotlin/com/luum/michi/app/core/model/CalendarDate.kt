package com.luum.michi.app.core.model

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

/**
 * Maps M3 date-picker millis (a UTC calendar date) to the local-midnight
 * bucket. The picker speaks UTC: Oct 30 00:00 UTC picked in a UTC-6 zone is
 * still Oct 29 locally, so interpreting the millis in the local zone shifts
 * the selection a day behind. Read the calendar date in UTC, then midnight
 * it in the local zone.
 */
internal fun utcMillisToLocalMidnightBucket(millis: Long, timeZone: TimeZone): Long {
    val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
    return date.atStartOfDayIn(timeZone).epochSeconds
}
