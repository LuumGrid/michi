package com.luum.michi.app.core.media.repository

import com.luum.michi.app.core.media.domain.MediaReleaseDateTime
import com.luum.michi.app.core.media.domain.toLocalMediaReleaseDateTime
import com.luum.michi.app.core.media.repository.dto.MediaNextAiringEpisodeDto

internal fun MediaNextAiringEpisodeDto?.toMediaReleaseDateTime(): MediaReleaseDateTime? {
    if (this == null) return null
    return airingAt.toLocalMediaReleaseDateTime()
}
