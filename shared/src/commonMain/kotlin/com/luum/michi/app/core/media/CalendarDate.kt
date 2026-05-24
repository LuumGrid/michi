package com.luum.michi.app.core.media

internal data class CalendarDateParts(
    val year: Int,
    val month: Int,
    val day: Int,
)

internal expect fun millisToCalendarParts(millis: Long): CalendarDateParts

internal expect fun calendarPartsToMillis(parts: CalendarDateParts): Long
