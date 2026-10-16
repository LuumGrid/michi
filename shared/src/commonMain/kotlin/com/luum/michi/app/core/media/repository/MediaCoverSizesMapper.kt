package com.luum.michi.app.core.media.repository

import com.luum.michi.app.core.media.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.model.CoverSizes

/** Media cover variants (extraLarge/large/medium). */
internal fun MediaCoverImageDto?.toCoverSizes(): CoverSizes? = this?.let {
    CoverSizes(medium = it.medium, large = it.large, extraLarge = it.extraLarge)
}
