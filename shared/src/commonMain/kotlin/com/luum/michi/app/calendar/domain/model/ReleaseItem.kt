package com.luum.michi.app.calendar.domain.model

data class ReleaseItem(
    val title: String,
    val release: String,
    val time: String,
    val paletteHex: String?,
    val id: Int? = null,
    val coverUrl: String? = null,
    val averageScore: Int? = null,
    val favourites: Int? = null,
    val popularity: Int? = null,
    val isUserFavorited: Boolean = false,
    val isUserRanked: Boolean = false,
    val userStatus: String? = null,
    val streamingPlatforms: List<StreamingPlatform> = emptyList(),
)

data class StreamingPlatform(
    val site: String,
    val url: String,
    val iconUrl: String? = null,
    val color: String? = null,
)
