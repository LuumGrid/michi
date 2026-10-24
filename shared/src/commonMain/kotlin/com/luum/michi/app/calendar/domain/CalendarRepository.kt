package com.luum.michi.app.calendar.domain

import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.calendar.domain.model.ReleaseItem

internal data class CalendarEntry(
    val scheduleId: Int,
    val item: ReleaseItem,
)

internal interface CalendarRepository {
    /**
     * Single-day slice of the airing schedule: one query per chosen day
     * (typically a single page), so day navigation never fans out into
     * the multi-page loop a wide window needs. Window is [from, to] in
     * epoch seconds; callers pass a local-midnight day slice.
     */
    suspend fun loadDay(from: Long, to: Long): NetworkResult<List<CalendarEntry>>
}
