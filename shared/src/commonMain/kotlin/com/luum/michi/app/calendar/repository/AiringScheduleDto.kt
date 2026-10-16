package com.luum.michi.app.calendar.repository

import com.luum.michi.app.core.network.repository.dto.MediaDto
import com.luum.michi.app.core.network.repository.dto.PageInfoDto
import kotlinx.serialization.Serializable

/** Airing-schedule page shape; only the calendar decodes it. */
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
