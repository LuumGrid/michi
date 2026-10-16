package com.luum.michi.app.core.model

/**
 * One media cell of a detail poster grid (studio works, genre works). Carries
 * only primitives plus [CoverSizes], so the shared grid renders it without
 * knowing which page produced it. `viewerStatus` is the raw AniList list status
 * (null for guests), used by the card's status badge.
 */
internal data class MediaGridItem(
    val mediaId: Int,
    val title: String,
    val cover: CoverSizes?,
    val paletteHex: String?,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
    val viewerStatus: String?,
    val favourites: Int? = null,
    val genres: List<String> = emptyList(),
)

/** A page of [MediaGridItem]s plus its pagination cursor. */
internal data class MediaGridPage(
    val items: List<MediaGridItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)
