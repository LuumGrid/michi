package com.luum.michi.app.discovery.data

import com.luum.michi.app.core.anilist.dto.AiringScheduleDto
import com.luum.michi.app.core.anilist.dto.DiscoveryResponseDto
import com.luum.michi.app.core.anilist.dto.MediaDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.platform.components.PlatformHomeMediaItem
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem
import com.luum.michi.app.core.platform.hexToPalette

internal fun DiscoveryResponseDto.toDiscoveryFeed(): DiscoveryFeed = DiscoveryFeed(
    releasingToday = releasingToday?.airingSchedules
        ?.mapNotNull { it.toReleaseItem() }
        .orEmpty(),
    trendingAnimation = trendingAnime?.media
        ?.map { it.toMediaItem(metaFor = ::animeMeta) }
        .orEmpty(),
    trendingReading = trendingManga?.media
        ?.map { it.toMediaItem(metaFor = ::mangaMeta) }
        .orEmpty(),
)

private fun MediaDto.toMediaItem(metaFor: (MediaDto) -> String): PlatformHomeMediaItem =
    PlatformHomeMediaItem(
        title = title.bestTitle(),
        meta = metaFor(this),
        colors = hexToPalette(coverImage?.color),
        id = id,
    )

private fun AiringScheduleDto.toReleaseItem(): PlatformHomeReleaseItem? {
    val media = media ?: return null
    return PlatformHomeReleaseItem(
        title = media.title.bestTitle(),
        release = "Ep ${episode}",
        time = formatAiringTime(airingAt),
        colors = hexToPalette(media.coverImage?.color),
        id = media.id,
    )
}

private fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

private fun animeMeta(media: MediaDto): String {
    val format = formatLabel(media.format) ?: "Anime"
    val episodes = media.episodes?.takeIf { it > 0 }?.let { " · $it ep" }.orEmpty()
    return format + episodes
}

private fun mangaMeta(media: MediaDto): String {
    val format = formatLabel(media.format) ?: "Manga"
    val chapters = media.chapters?.takeIf { it > 0 }?.let { " · $it ch" }.orEmpty()
    return format + chapters
}

private fun formatLabel(format: String?): String? = format
    ?.replace("_", " ")
    ?.lowercase()
    ?.replaceFirstChar { it.uppercase() }

private fun formatAiringTime(airingAtEpoch: Long): String {
    val secondsInDay = ((airingAtEpoch % SecondsPerDay) + SecondsPerDay) % SecondsPerDay
    val hour = (secondsInDay / 3600L).toInt()
    val minute = ((secondsInDay % 3600L) / 60L).toInt()
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private const val SecondsPerDay: Long = 86_400L
