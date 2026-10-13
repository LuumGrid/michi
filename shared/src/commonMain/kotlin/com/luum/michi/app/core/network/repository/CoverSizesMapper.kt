package com.luum.michi.app.core.network.repository

import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.network.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.network.repository.dto.PersonImageDto

/** Media cover variants (extraLarge/large/medium). */
internal fun MediaCoverImageDto?.toCoverSizes(): CoverSizes? = this?.let {
    CoverSizes(medium = it.medium, large = it.large, extraLarge = it.extraLarge)
}

/** People images (character/staff) ship only large/medium — no extraLarge. */
internal fun PersonImageDto?.toCoverSizes(): CoverSizes? = this?.let {
    CoverSizes(medium = it.medium, large = it.large)
}
