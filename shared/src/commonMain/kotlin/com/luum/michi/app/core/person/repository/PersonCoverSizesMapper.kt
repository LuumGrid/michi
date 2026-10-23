package com.luum.michi.app.core.person.repository

import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.person.repository.dto.PersonImageDto
import com.luum.michi.app.core.util.isAniListDefaultImage

/**
 * People images (character/staff) ship only large/medium — no extraLarge. A
 * person without an image comes back as AniList's shared placeholder
 * (`.../anilistcdn/{character,staff}/{large,medium}/default.jpg`, verified
 * against the live API), which is mapped to null so the UI paints its own
 * placeholder.
 */
internal fun PersonImageDto?.toCoverSizes(): CoverSizes? =
    this?.let { personCoverSizes(large = it.large, medium = it.medium) }

/** [CoverSizes] for a person image, dropping AniList's shared placeholder URLs. */
internal fun personCoverSizes(large: String?, medium: String?): CoverSizes? {
    val realLarge = large?.takeUnless { it.isAniListDefaultImage() }
    val realMedium = medium?.takeUnless { it.isAniListDefaultImage() }
    return if (realLarge == null && realMedium == null) {
        null
    } else {
        CoverSizes(medium = realMedium, large = realLarge)
    }
}
