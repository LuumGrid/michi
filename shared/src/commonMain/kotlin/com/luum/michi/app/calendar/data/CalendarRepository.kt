package com.luum.michi.app.calendar.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem

internal data class CalendarDay(
    val dayBucket: Long,
    val isoDayOfWeek: Int,
    val offsetFromToday: Int,
    val items: List<PlatformHomeReleaseItem>,
)

internal data class CalendarFeed(val days: List<CalendarDay>)

internal interface CalendarRepository {
    suspend fun loadFeed(): NetworkResult<CalendarFeed>
}
