package com.luum.michi.app.core.repository.anilist

import com.luum.michi.app.core.domain.model.MediaReleaseDateTime
import com.luum.michi.app.core.domain.model.toLocalMediaReleaseDateTime
import com.luum.michi.app.core.repository.anilist.dto.MediaNextAiringEpisodeDto

internal fun MediaNextAiringEpisodeDto?.toMediaReleaseDateTime(): MediaReleaseDateTime? {
    if (this == null) return null
    return airingAt.toMediaReleaseDateTime()
}

internal fun Long.toMediaReleaseDateTime(): MediaReleaseDateTime {
    return this.toLocalMediaReleaseDateTime()
}
