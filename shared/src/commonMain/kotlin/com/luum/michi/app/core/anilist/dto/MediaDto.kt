package com.luum.michi.app.core.anilist.dto

import kotlinx.serialization.Serializable

/**
 * AniList `Media` JSON shape used across queries (lists, favorites, detail).
 *
 * Only the fields Michi consumes today are declared; AniList returns many more.
 */
@Serializable
internal data class MediaDto(
    val id: Int,
    val title: MediaTitleDto? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val coverImage: MediaCoverImageDto? = null,
    val bannerImage: String? = null,
    val averageScore: Int? = null,
    val favourites: Int? = null,
    val popularity: Int? = null,
    val genres: List<String>? = null,
    val nextAiringEpisode: MediaNextAiringEpisodeDto? = null,
    val isFavourite: Boolean? = null,
    val mediaListEntry: MediaViewerListEntryDto? = null,
    val trending: Int? = null,
    val startDate: FuzzyDateDto? = null,
    val externalLinks: List<MediaExternalLinkDto>? = null,
)

@Serializable
internal data class MediaExternalLinkDto(
    val id: Int? = null,
    val site: String? = null,
    val url: String? = null,
    val type: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val language: String? = null,
    val isDisabled: Boolean? = null,
)

@Serializable
internal data class MediaTitleDto(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
    val userPreferred: String? = null,
)

@Serializable
internal data class MediaCoverImageDto(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
    val color: String? = null,
) {
    /** Full-resolution cover. Use only for the hero image in MediaDetail. */
    val bestUrl: String? get() = extraLarge ?: large ?: medium

    /** Smaller cover for thumbnails in rails, grids, and lists.
     *  Prefers `large` (roughly 230px) over `extraLarge` (460px) to reduce
     *  payload in layouts where the image is displayed small. */
    val thumbnailUrl: String? get() = large ?: medium ?: extraLarge
}

@Serializable
internal data class MediaNextAiringEpisodeDto(
    val episode: Int,
    val airingAt: Long,
    val timeUntilAiring: Long? = null,
)
