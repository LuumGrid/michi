package com.luum.michi.app.core.network.repository.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Every dashboard rail alias (anime + manga). Each query only sends its own
 * type's aliases, so the rest decode as null.
 */
@Serializable
internal data class DashboardResponseDto(
    @SerialName("seasonal") val seasonal: MediaPageDto? = null,
    @SerialName("upcomingSeason") val upcomingSeason: MediaPageDto? = null,
    @SerialName("trendingAnime") val trendingAnime: MediaPageDto? = null,
    @SerialName("popularAnime") val popularAnime: MediaPageDto? = null,
    @SerialName("popularMovie") val popularMovie: MediaPageDto? = null,
    @SerialName("bestAnime") val bestAnime: MediaPageDto? = null,
    @SerialName("bestMovie") val bestMovie: MediaPageDto? = null,
    @SerialName("bestAnimeNow") val bestAnimeNow: MediaPageDto? = null,
    @SerialName("bestMovieNow") val bestMovieNow: MediaPageDto? = null,
    @SerialName("newReleasesManga") val newReleasesManga: MediaPageDto? = null,
    @SerialName("trendingManga") val trendingManga: MediaPageDto? = null,
    @SerialName("popularManga") val popularManga: MediaPageDto? = null,
    @SerialName("popularLightNovel") val popularLightNovel: MediaPageDto? = null,
    @SerialName("popularManhwa") val popularManhwa: MediaPageDto? = null,
    @SerialName("popularManhua") val popularManhua: MediaPageDto? = null,
    @SerialName("bestManga") val bestManga: MediaPageDto? = null,
    @SerialName("bestLightNovel") val bestLightNovel: MediaPageDto? = null,
    @SerialName("bestManhwa") val bestManhwa: MediaPageDto? = null,
    @SerialName("bestManhua") val bestManhua: MediaPageDto? = null,
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
