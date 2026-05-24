package com.luum.michi.app.core.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shape for the `Page.media(search:)` search query.
 */
@Serializable
internal data class MediaSearchResponseDto(
    @SerialName("Page") val page: MediaSearchPageDto? = null,
)

@Serializable
internal data class MediaSearchPageDto(
    val media: List<MediaSearchItemDto> = emptyList(),
)

@Serializable
internal data class MediaSearchItemDto(
    val id: Int,
    val type: String? = null,
    val title: MediaTitleDto? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val chapters: Int? = null,
    val averageScore: Int? = null,
    val coverImage: MediaCoverImageDto? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val startDate: FuzzyDateDto? = null,
    val isAdult: Boolean? = null,
)
