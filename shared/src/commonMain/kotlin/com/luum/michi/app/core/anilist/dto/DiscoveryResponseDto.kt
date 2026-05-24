package com.luum.michi.app.core.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Combined response for the discovery (Home) screen — pulls trending anime,
 * trending manga, and the next 24h of airing schedules in a single round trip.
 *
 * Sample GraphQL:
 * ```
 * query Discovery($from: Int!, $to: Int!) {
 *   trendingAnime: Page(perPage: 20) {
 *     media(sort: TRENDING_DESC, type: ANIME) { id title {…} format episodes coverImage {…} }
 *   }
 *   trendingManga: Page(perPage: 20) {
 *     media(sort: TRENDING_DESC, type: MANGA) { id title {…} format chapters coverImage {…} }
 *   }
 *   releasingToday: Page(perPage: 25) {
 *     airingSchedules(airingAt_greater: $from, airingAt_lesser: $to, sort: TIME) {
 *       id airingAt episode media { id title {…} format coverImage {…} }
 *     }
 *   }
 * }
 * ```
 */
@Serializable
internal data class DiscoveryResponseDto(
    @SerialName("trendingAnime")
    val trendingAnime: MediaPageDto? = null,
    @SerialName("trendingManga")
    val trendingManga: MediaPageDto? = null,
    @SerialName("releasingToday")
    val releasingToday: AiringSchedulePageDto? = null,
)

@Serializable
internal data class MediaPageDto(
    val media: List<MediaDto> = emptyList(),
)

@Serializable
internal data class AiringSchedulePageDto(
    val airingSchedules: List<AiringScheduleDto> = emptyList(),
)

@Serializable
internal data class AiringScheduleDto(
    val id: Int,
    val airingAt: Long,
    val episode: Int,
    val media: MediaDto? = null,
)
