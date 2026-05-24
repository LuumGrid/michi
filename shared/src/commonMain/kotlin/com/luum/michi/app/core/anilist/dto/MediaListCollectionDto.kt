package com.luum.michi.app.core.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shape for the `MediaListCollection` query — the authenticated user's
 * full anime or manga list grouped by status.
 */
@Serializable
internal data class MediaListCollectionResponseDto(
    @SerialName("MediaListCollection")
    val collection: MediaListCollectionDto,
)

@Serializable
internal data class MediaListCollectionDto(
    val lists: List<MediaListGroupDto> = emptyList(),
)

@Serializable
internal data class MediaListGroupDto(
    val name: String? = null,
    val status: String? = null,
    val isCustomList: Boolean = false,
    val entries: List<MediaListEntryDto> = emptyList(),
)

@Serializable
internal data class MediaListEntryDto(
    val id: Int,
    val status: String? = null,
    val score: Double = 0.0,
    val progress: Int = 0,
    val progressVolumes: Int? = null,
    val notes: String? = null,
    val updatedAt: Long? = null,
    val startedAt: FuzzyDateDto? = null,
    val completedAt: FuzzyDateDto? = null,
    @SerialName("private")
    val isPrivate: Boolean = false,
    val hiddenFromStatusLists: Boolean = false,
    val media: MediaDto,
)

@Serializable
internal data class FuzzyDateDto(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
)
