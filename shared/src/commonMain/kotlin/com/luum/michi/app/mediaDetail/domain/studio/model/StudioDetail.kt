package com.luum.michi.app.mediaDetail.domain.studio.model


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
    val coverUrl: String?,
    val paletteHex: String?,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
    val viewerStatus: String?,
)

internal data class StudioMediaPage(
    val items: List<StudioMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal enum class StudioMediaSort(val apiValue: String) {
    POPULARITY("POPULARITY_DESC"),
    NEWEST("START_DATE_DESC"),
    OLDEST("START_DATE"),
    FAVOURITES("FAVOURITES_DESC"),
    SCORE("SCORE_DESC"),
}
