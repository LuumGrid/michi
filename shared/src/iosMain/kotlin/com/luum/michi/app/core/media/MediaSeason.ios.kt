package com.luum.michi.app.core.media

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970

internal actual fun currentSeasonAndYear(): MediaSeasonYear {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth,
        fromDate = NSDate(),
    )
    return MediaSeasonYear(
        season = seasonForMonth(components.month.toInt()),
        year = components.year.toInt(),
    )
}

internal actual fun isoDayOfWeek(epochSeconds: Long): Int {
    val date = NSDate.dateWithTimeIntervalSince1970(epochSeconds.toDouble())
    val components = NSCalendar.currentCalendar.components(NSCalendarUnitWeekday, fromDate = date)
    // NSCalendar weekday: 1=Sunday..7=Saturday. Convert to ISO 1=Monday..7=Sunday.
    val weekday = components.weekday.toInt()
    return if (weekday == 1) 7 else weekday - 1
}
