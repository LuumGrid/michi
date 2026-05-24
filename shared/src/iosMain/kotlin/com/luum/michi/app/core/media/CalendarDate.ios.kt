package com.luum.michi.app.core.media

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeZoneForSecondsFromGMT

private val utcCalendar: NSCalendar
    get() {
        val c = NSCalendar.currentCalendar
        c.timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
        return c
    }

internal actual fun millisToCalendarParts(millis: Long): CalendarDateParts {
    val date = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
    val components = utcCalendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
        fromDate = date,
    )
    return CalendarDateParts(
        year = components.year.toInt(),
        month = components.month.toInt(),
        day = components.day.toInt(),
    )
}

internal actual fun calendarPartsToMillis(parts: CalendarDateParts): Long {
    val components = NSDateComponents().apply {
        year = parts.year.toLong()
        month = parts.month.toLong()
        day = parts.day.toLong()
        timeZone = NSTimeZone.timeZoneForSecondsFromGMT(0)
    }
    val date = utcCalendar.dateFromComponents(components) ?: return 0L
    return (date.timeIntervalSince1970 * 1000.0).toLong()
}

internal actual fun Long.toLocalMediaReleaseDateTime(): com.luum.michi.app.core.model.MediaReleaseDateTime {
    val date = NSDate.dateWithTimeIntervalSince1970(this.toDouble())
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(
        platform.Foundation.NSCalendarUnitYear or platform.Foundation.NSCalendarUnitMonth or platform.Foundation.NSCalendarUnitDay or platform.Foundation.NSCalendarUnitHour or platform.Foundation.NSCalendarUnitMinute,
        fromDate = date
    )
    return com.luum.michi.app.core.model.MediaReleaseDateTime(
        day = components.day.toInt(),
        month = components.month.toInt(),
        year = components.year.toInt(),
        hour = components.hour.toInt(),
        minute = components.minute.toInt()
    )
}
