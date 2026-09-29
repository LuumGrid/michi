package com.luum.michi.app.calendar.repository

import com.luum.michi.app.core.network.repository.dto.AiringScheduleDto
import com.luum.michi.app.core.network.repository.dto.MediaExternalLinkDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.model.parseMediaFormat
import com.luum.michi.app.core.model.parseMediaSeason
import com.luum.michi.app.core.model.parseMediaWorkStatus
import com.luum.michi.app.calendar.domain.model.ReleaseItem
import com.luum.michi.app.calendar.domain.model.StreamingPlatform
import com.luum.michi.app.core.model.toLocalMediaReleaseDateTime
import com.luum.michi.app.core.medialist.domain.parseMediaListStatus

internal fun AiringScheduleDto.toReleaseItemOrNull(): ReleaseItem? {
    val media = media ?: return null
    val hasUserScore = (media.mediaListEntry?.score ?: 0.0) > 0.0
    val releaseLabel = "Ep. $episode"
    return ReleaseItem(
        title = media.title.bestTitle(),
        release = releaseLabel,
        time = formatAiringTime(airingAt),
        airingAtEpoch = airingAt,
        paletteHex = media.coverImage?.color,
        id = media.id,
        coverUrl = media.coverImage?.thumbnailUrl,
        format = parseMediaFormat(media.format),
        mediaStatus = parseMediaWorkStatus(media.status),
        averageScore = media.averageScore,
        favourites = media.favourites,
        popularity = media.popularity,
        isUserFavorited = media.isFavourite ?: false,
        isUserRanked = hasUserScore,
        userScore = media.mediaListEntry?.score?.toFloat(),
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

private fun formatAiringTime(airingAtEpoch: Long): String {
    val localDateTime = airingAtEpoch.toLocalMediaReleaseDateTime()
    return "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}
