package com.luum.michi.app.core.media

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import com.luum.michi.app.core.model.MediaReleaseDateTime

internal actual fun millisToCalendarParts(millis: Long): CalendarDateParts {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    return CalendarDateParts(date.year, date.monthValue, date.dayOfMonth)
}

internal actual fun calendarPartsToMillis(parts: CalendarDateParts): Long =
    LocalDate.of(parts.year, parts.month, parts.day)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

internal actual fun Long.toLocalMediaReleaseDateTime(): MediaReleaseDateTime {
    val zonedDateTime = Instant.ofEpochSecond(this).atZone(ZoneId.systemDefault())
    return MediaReleaseDateTime(
        day = zonedDateTime.dayOfMonth,
        month = zonedDateTime.monthValue,
        year = zonedDateTime.year,
        hour = zonedDateTime.hour,
        minute = zonedDateTime.minute
    )
}
