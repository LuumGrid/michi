package com.luum.michi.app.core.media.domain

import com.luum.michi.app.core.model.CoverSizes

/**
 * One media cell of a detail poster grid (studio works, genre works). Carries
 * only primitives plus [CoverSizes], so the shared grid renders it without
 * knowing which page produced it.
 */
internal data class MediaGridItem(
    val mediaId: Int,
    val title: String,
    val cover: CoverSizes?,
    val paletteHex: String?,
    val year: Int?,
    val genres: List<String> = emptyList(),
)
