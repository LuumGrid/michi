package com.luum.michi.app.core.network.repository

import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.network.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.network.repository.dto.PersonImageDto

/** Media cover variants (extraLarge/large/medium). */
internal fun MediaCoverImageDto?.toCoverSizes(): CoverSizes? = this?.let {
    CoverSizes(medium = it.medium, large = it.large, extraLarge = it.extraLarge)
}

/**
 * People images (character/staff) ship only large/medium — no extraLarge. A
 * person without an image comes back as AniList's shared placeholder
 * (`.../anilistcdn/{character,staff}/{large,medium}/default.jpg`, verified
 * against the live API), which is mapped to null so the UI paints its own grey
 * placeholder instead of the blue "NO IMAGE" art.
 */
internal fun PersonImageDto?.toCoverSizes(): CoverSizes? =
    this?.let { personCoverSizes(large = it.large, medium = it.medium) }

/** [CoverSizes] for a person image, dropping AniList's shared placeholder URLs. */
internal fun personCoverSizes(large: String?, medium: String?): CoverSizes? {
    val realLarge = large?.takeUnless { it.isDefaultPersonImage() }
    val realMedium = medium?.takeUnless { it.isDefaultPersonImage() }
    return if (realLarge == null && realMedium == null) {
        null
    } else {
        CoverSizes(medium = realMedium, large = realLarge)
    }
}

/** AniList's "no image" art always ends in `/default.jpg`. */
private fun String.isDefaultPersonImage(): Boolean =
    endsWith(DefaultPersonImageSuffix, ignoreCase = true)

private const val DefaultPersonImageSuffix = "/default.jpg"
