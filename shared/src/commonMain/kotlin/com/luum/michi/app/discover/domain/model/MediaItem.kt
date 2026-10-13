package com.luum.michi.app.discover.domain.model
import com.luum.michi.app.core.model.CoverSizes

internal data class MediaItem(
    val title: String,
    val meta: String,
    val paletteHex: String?,
    val id: Int? = null,
    val cover: CoverSizes? = null,
    val averageScore: Int? = null,
    val favourites: Int? = null,
    val isUserFavorited: Boolean = false,
    val isUserRanked: Boolean = false,
)
