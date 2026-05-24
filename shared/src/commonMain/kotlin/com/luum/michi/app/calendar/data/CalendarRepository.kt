package com.luum.michi.app.calendar.data

import kotlinx.coroutines.flow.Flow
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem

internal data class CalendarEntry(
    val scheduleId: Int,
    val item: PlatformHomeReleaseItem,
)

internal data class CalendarDay(
    val dayBucket: Long,
    val isoDayOfWeek: Int,
    val offsetFromToday: Int,
    val day: Int,
    val month: Int,
    val year: Int,
    val items: List<CalendarEntry>,
)

internal data class CalendarFeed(val days: List<CalendarDay>)

internal interface CalendarRepository {
    fun loadFeed(): Flow<NetworkResult<CalendarFeed>>
}
