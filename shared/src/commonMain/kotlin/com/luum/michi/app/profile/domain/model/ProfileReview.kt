package com.luum.michi.app.profile.domain.model

import com.luum.michi.app.core.model.CoverSizes

/**
 * One of the viewer's media reviews. Tapping it opens the reviewed media's
 * detail ([mediaId]); [score] is the review's 0-100 rating.
 */
internal data class ProfileReview(
    val id: Int,
    val mediaId: Int,
    val mediaTitle: String,
    val mediaCover: CoverSizes?,
    val paletteHex: String?,
    val summary: String,
    val score: Int,
)
