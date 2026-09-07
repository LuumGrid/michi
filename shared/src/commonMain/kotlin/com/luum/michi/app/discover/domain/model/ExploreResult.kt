package com.luum.michi.app.discover.domain.model


internal data class ExploreResult(
    val id: Int,
    val title: String,
    val meta: String,
    val coverUrl: String?,
    val paletteHex: String?,
    val averageScore: Int?,
    val favourites: Int? = null,
    val genres: List<String> = emptyList(),
    val isUserFavorited: Boolean = false,
    val isUserRanked: Boolean = false,
)
