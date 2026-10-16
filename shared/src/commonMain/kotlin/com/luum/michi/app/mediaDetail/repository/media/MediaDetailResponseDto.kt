package com.luum.michi.app.mediaDetail.repository.media

import com.luum.michi.app.core.network.repository.dto.FuzzyDateDto
import com.luum.michi.app.core.network.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.network.repository.dto.MediaExternalLinkDto
import com.luum.michi.app.core.network.repository.dto.MediaNextAiringEpisodeDto
import com.luum.michi.app.core.network.repository.dto.MediaTitleDto
import com.luum.michi.app.core.network.repository.dto.MediaViewerListEntryDto
import com.luum.michi.app.core.network.repository.dto.PageInfoDto
import com.luum.michi.app.core.network.repository.dto.PersonImageDto
import com.luum.michi.app.core.network.repository.dto.PersonNameDto
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
 *     studios(sort: [NAME]) { edges { isMain node { id name isAnimationStudio } } }
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
    val studios: MediaStudioConnectionDto? = null,
    val nextAiringEpisode: MediaNextAiringEpisodeDto? = null,
    val countryOfOrigin: String? = null,
    val isAdult: Boolean? = null,
    val isFavourite: Boolean? = null,
    val mediaListEntry: MediaViewerListEntryDto? = null,
    val relations: MediaRelationConnectionDto? = null,
    val characters: MediaCharacterConnectionDto? = null,
    val staff: MediaStaffConnectionDto? = null,
    val tags: List<MediaTagDto> = emptyList(),
    val externalLinks: List<MediaExternalLinkDto> = emptyList(),
    val trailer: MediaTrailerDto? = null,
    val synonyms: List<String> = emptyList(),
)

@Serializable
internal data class MediaStudioConnectionDto(
    val edges: List<MediaStudioEdgeDto> = emptyList(),
)

@Serializable
internal data class MediaStudioEdgeDto(
    val isMain: Boolean = false,
    val node: MediaStudioNodeDto? = null,
)

@Serializable
internal data class MediaStudioNodeDto(
    val id: Int,
    val name: String,
    val isAnimationStudio: Boolean = false,
)

@Serializable
internal data class MediaTagDto(
    val name: String? = null,
    val rank: Int = 0,
    val isMediaSpoiler: Boolean = false,
)

@Serializable
internal data class MediaTrailerDto(
    val id: String? = null,
    val site: String? = null,
    val thumbnail: String? = null,
)

@Serializable
internal data class MediaCharacterConnectionDto(
    val pageInfo: PageInfoDto? = null,
    val edges: List<MediaCharacterEdgeDto> = emptyList(),
)

@Serializable
internal data class MediaCharacterEdgeDto(
    val id: Int? = null,
    val role: String? = null,
    val node: MediaCharacterNodeDto? = null,
)

@Serializable
internal data class MediaCharacterNodeDto(
    val id: Int,
    val name: PersonNameDto? = null,
    val image: PersonImageDto? = null,
)

@Serializable
internal data class MediaStaffConnectionDto(
    val pageInfo: PageInfoDto? = null,
    val edges: List<MediaStaffEdgeDto> = emptyList(),
)

@Serializable
internal data class MediaStaffEdgeDto(
    val id: Int? = null,
    val role: String? = null,
    val node: MediaStaffNodeDto? = null,
)

@Serializable
internal data class MediaStaffNodeDto(
    val id: Int,
    val name: PersonNameDto? = null,
    val image: PersonImageDto? = null,
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
    val format: String? = null,
    val status: String? = null,
    val title: MediaTitleDto? = null,
    val coverImage: MediaCoverImageDto? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val startDate: FuzzyDateDto? = null,
)
