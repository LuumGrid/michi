package com.luum.michi.app.discover.domain.model

data class MediaItem(
    val title: String,
    val meta: String,
    val paletteHex: String?,
    val id: Int? = null,
    val coverUrl: String? = null,
    val averageScore: Int? = null,
    val favourites: Int? = null,
    val isUserFavorited: Boolean = false,
    val isUserRanked: Boolean = false,
)
