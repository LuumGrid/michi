package com.luum.michi.app.core.media.repository

import com.luum.michi.app.core.media.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.util.isAniListDefaultImage

/**
 * Media cover variants (extraLarge/large/medium). AniList's shared "no image"
 * art maps to null so every surface shows the same placeholder.
 */
internal fun MediaCoverImageDto?.toCoverSizes(): CoverSizes? = this?.let {
    val medium = it.medium?.takeUnless { url -> url.isAniListDefaultImage() }
    val large = it.large?.takeUnless { url -> url.isAniListDefaultImage() }
    val extraLarge = it.extraLarge?.takeUnless { url -> url.isAniListDefaultImage() }
    if (medium == null && large == null && extraLarge == null) {
        null
    } else {
        CoverSizes(medium = medium, large = large, extraLarge = extraLarge)
    }
}
