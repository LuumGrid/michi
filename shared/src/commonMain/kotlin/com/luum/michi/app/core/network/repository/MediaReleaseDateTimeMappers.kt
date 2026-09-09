package com.luum.michi.app.core.network.repository

import com.luum.michi.app.core.model.MediaReleaseDateTime
import com.luum.michi.app.core.model.toLocalMediaReleaseDateTime
import com.luum.michi.app.core.network.repository.dto.MediaNextAiringEpisodeDto

internal fun MediaNextAiringEpisodeDto?.toMediaReleaseDateTime(): MediaReleaseDateTime? {
    if (this == null) return null
    return airingAt.toMediaReleaseDateTime()
}

internal fun Long.toMediaReleaseDateTime(): MediaReleaseDateTime {
    return this.toLocalMediaReleaseDateTime()
}
