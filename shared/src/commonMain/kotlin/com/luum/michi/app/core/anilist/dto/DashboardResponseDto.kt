package com.luum.michi.app.core.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DashboardResponseDto(
    @SerialName("trendingAnime") val trendingAnime: MediaPageDto? = null,
    @SerialName("trendingManga") val trendingManga: MediaPageDto? = null,
    @SerialName("thisSeason") val popularThisSeason: MediaPageDto? = null,
    @SerialName("upcomingNextSeason") val upcomingNextSeason: MediaPageDto? = null,
    @SerialName("allTimePopularAnime") val allTimePopularAnime: MediaPageDto? = null,
    @SerialName("allTimePopularManga") val allTimePopularManga: MediaPageDto? = null,
    @SerialName("topAnime") val topAnime: MediaPageDto? = null,
    @SerialName("topManga") val topManga: MediaPageDto? = null,
)

@Serializable
internal data class MediaPageDto(
    val media: List<MediaDto> = emptyList(),
)

@Serializable
internal data class AiringSchedulePageDto(
    val pageInfo: MediaPageInfoDto? = null,
    val airingSchedules: List<AiringScheduleDto> = emptyList(),
)

@Serializable
internal data class AiringScheduleDto(
    val id: Int,
    val airingAt: Long,
    val episode: Int,
    val media: MediaDto? = null,
)
