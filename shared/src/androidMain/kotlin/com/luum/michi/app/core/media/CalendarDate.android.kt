package com.luum.michi.app.core.media

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal actual fun millisToCalendarParts(millis: Long): CalendarDateParts {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    return CalendarDateParts(date.year, date.monthValue, date.dayOfMonth)
}

internal actual fun calendarPartsToMillis(parts: CalendarDateParts): Long =
    LocalDate.of(parts.year, parts.month, parts.day)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
