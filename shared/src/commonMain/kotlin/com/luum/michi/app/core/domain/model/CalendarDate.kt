package com.luum.michi.app.core.domain.model

internal data class CalendarDateParts(
    val year: Int,
    val month: Int,
    val day: Int,
)

internal expect fun millisToCalendarParts(millis: Long): CalendarDateParts

internal expect fun calendarPartsToMillis(parts: CalendarDateParts): Long

internal expect fun Long.toLocalMediaReleaseDateTime(): MediaReleaseDateTime

internal expect fun localMidnightEpoch(epochSeconds: Long): Long
