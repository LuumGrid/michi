package com.luum.michi.app.core.network.repository.dto

import kotlinx.serialization.Serializable

/**
 * Airing-schedule page shape, shared by the calendar and the dashboard. Kept in
 * core because both features decode it.
 */
@Serializable
internal data class AiringSchedulePageDto(
    val pageInfo: PageInfoDto? = null,
    val airingSchedules: List<AiringScheduleDto> = emptyList(),
)

@Serializable
internal data class AiringScheduleDto(
    val id: Int,
    val airingAt: Long,
    val episode: Int,
    val media: MediaDto? = null,
)
