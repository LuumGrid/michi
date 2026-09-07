package com.luum.michi.app.core.model

import com.luum.michi.app.core.anilist.dto.MediaNextAiringEpisodeDto

internal fun MediaNextAiringEpisodeDto?.toMediaReleaseDateTime(): MediaReleaseDateTime? {
    if (this == null) return null
    return airingAt.toMediaReleaseDateTime()
}

internal fun Long.toMediaReleaseDateTime(): MediaReleaseDateTime {
    return this.toLocalMediaReleaseDateTime()
}
