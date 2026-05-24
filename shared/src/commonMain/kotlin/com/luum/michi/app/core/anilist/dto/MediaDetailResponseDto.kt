package com.luum.michi.app.core.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shape for the `Media(id)` detail query.
 *
 * Sample:
 * ```
 * query MediaDetail($id: Int!) {
 *   Media(id: $id) {
 *     id type title { ... } description format status episodes chapters
 *     volumes duration genres averageScore meanScore popularity favourites
 *     coverImage { ... } bannerImage source season seasonYear
 *     startDate { year month day } endDate { year month day }
 *     studios(isMain: true) { nodes { id name } }
 *     nextAiringEpisode { episode airingAt timeUntilAiring }
 *     countryOfOrigin isAdult
 *   }
 * }
 * ```
 */
@Serializable
internal data class MediaDetailResponseDto(
    @SerialName("Media") val media: MediaDetailDto? = null,
)

@Serializable
internal data class MediaDetailDto(
    val id: Int,
    val type: String? = null,
    val title: MediaTitleDto? = null,
    val description: String? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val duration: Int? = null,
    val genres: List<String> = emptyList(),
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val coverImage: MediaCoverImageDto? = null,
    val bannerImage: String? = null,
    val source: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val startDate: FuzzyDateDto? = null,
    val endDate: FuzzyDateDto? = null,
    val studios: StudioConnectionDto? = null,
    val nextAiringEpisode: MediaNextAiringEpisodeDto? = null,
    val countryOfOrigin: String? = null,
    val isAdult: Boolean? = null,
    val isFavourite: Boolean? = null,
    val mediaListEntry: MediaViewerListEntryDto? = null,
    val relations: MediaRelationConnectionDto? = null,
)

@Serializable
internal data class MediaRelationConnectionDto(
    val edges: List<MediaRelationEdgeDto> = emptyList(),
)

@Serializable
internal data class MediaRelationEdgeDto(
    val relationType: String? = null,
    val node: MediaRelationNodeDto? = null,
)

@Serializable
internal data class MediaRelationNodeDto(
    val id: Int,
    val type: String? = null,
    val format: String? = null,
    val title: MediaTitleDto? = null,
    val coverImage: MediaCoverImageDto? = null,
)

@Serializable
internal data class MediaViewerListEntryDto(
    val id: Int,
    val status: String? = null,
    val progress: Int = 0,
    val progressVolumes: Int? = null,
    val score: Double = 0.0,
    val notes: String? = null,
    val repeat: Int = 0,
    val priority: Int = 0,
    @SerialName("private") val isPrivate: Boolean = false,
    val hiddenFromStatusLists: Boolean = false,
    val startedAt: FuzzyDateDto? = null,
    val completedAt: FuzzyDateDto? = null,
)

