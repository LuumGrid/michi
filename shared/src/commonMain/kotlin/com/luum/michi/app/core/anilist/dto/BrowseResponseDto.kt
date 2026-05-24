package com.luum.michi.app.core.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Combined response for the Browse screen — pulls six curated rails in one
 * round trip: popular this season + next season (anime), all-time popular
 * anime/manga, and top-rated anime/manga.
 */
@Serializable
internal data class BrowseResponseDto(
    @SerialName("popularThisSeason") val popularThisSeason: MediaPageDto? = null,
    @SerialName("upcomingNextSeason") val upcomingNextSeason: MediaPageDto? = null,
    @SerialName("allTimePopularAnime") val allTimePopularAnime: MediaPageDto? = null,
    @SerialName("allTimePopularManga") val allTimePopularManga: MediaPageDto? = null,
    @SerialName("topAnime") val topAnime: MediaPageDto? = null,
    @SerialName("topManga") val topManga: MediaPageDto? = null,
)
