package com.luum.michi.app.discover.domain.model
import com.luum.michi.app.core.model.CoverSizes


internal data class ExploreResult(
    val id: Int,
    val title: String,
    val meta: String,
    val cover: CoverSizes?,
    val paletteHex: String?,
    val averageScore: Int?,
    val favourites: Int? = null,
    val genres: List<String> = emptyList(),
    val isUserFavorited: Boolean = false,
    val isUserRanked: Boolean = false,
    /** Studio results only: true = animation studio, false = producer. */
    val isAnimationStudio: Boolean = false,
)
