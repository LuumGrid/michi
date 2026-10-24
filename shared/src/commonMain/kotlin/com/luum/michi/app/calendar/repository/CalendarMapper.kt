package com.luum.michi.app.calendar.repository

import com.luum.michi.app.core.media.repository.dto.MediaExternalLinkDto
import com.luum.michi.app.core.media.repository.bestTitle
import com.luum.michi.app.core.media.domain.parseMediaFormat
import com.luum.michi.app.core.media.domain.parseMediaSeason
import com.luum.michi.app.core.media.domain.parseMediaWorkStatus
import com.luum.michi.app.calendar.domain.model.ReleaseItem
import com.luum.michi.app.calendar.domain.model.StreamingPlatform
import com.luum.michi.app.core.medialist.domain.parseMediaListStatus
import com.luum.michi.app.core.media.repository.toCoverSizes

internal fun AiringScheduleDto.toReleaseItemOrNull(): ReleaseItem? {
    val media = media ?: return null
    return ReleaseItem(
        title = media.title.bestTitle(),
        episode = episode,
        airingAtEpoch = airingAt,
        id = media.id,
        cover = media.coverImage?.toCoverSizes(),
        format = parseMediaFormat(media.format),
        mediaStatus = parseMediaWorkStatus(media.status),
        userStatus = parseMediaListStatus(media.mediaListEntry?.status),
        season = parseMediaSeason(media.season),
        seasonYear = media.seasonYear,
        streamingPlatforms = media.externalLinks
            ?.mapNotNull { it.toStreamingPlatform() }
            ?.distinctBy { it.site }
            .orEmpty(),
    )
}

private fun MediaExternalLinkDto.toStreamingPlatform(): StreamingPlatform? {
    if (isDisabled == true) return null
    if (type != "STREAMING") return null
    val site = site?.takeIf { it.isNotBlank() } ?: return null
    val url = url?.takeIf { it.isNotBlank() } ?: return null
    return StreamingPlatform(site = site, url = url, iconUrl = icon, color = color)
}
