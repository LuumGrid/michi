package com.luum.michi.app.mediaDetail.domain.studio.model

import com.luum.michi.app.core.model.CoverSizes


internal data class StudioDetail(
    val id: Int,
    val name: String,
    val isAnimeStudio: Boolean,
    val favourites: Int?,
    val isFavourite: Boolean,
    val media: StudioMediaPage,
)

internal data class StudioMediaItem(
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

internal data class StudioMediaPage(
    val items: List<StudioMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

/** Canonical AniList page for this studio. */
internal fun StudioDetail.aniListUrl(): String = "https://anilist.co/studio/$id"

/** Body used by the share sheet: name + canonical URL. */
internal fun StudioDetail.shareText(): String = "$name\n${aniListUrl()}"
