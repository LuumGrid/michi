package com.luum.michi.app.calendar.data

import com.luum.michi.app.core.anilist.dto.AiringScheduleDto
import com.luum.michi.app.core.anilist.dto.MediaExternalLinkDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.media.isoDayOfWeek
import com.luum.michi.app.core.media.localMidnightEpoch
import com.luum.michi.app.core.media.toLocalMediaReleaseDateTime
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem
import com.luum.michi.app.core.platform.components.StreamingPlatform
import com.luum.michi.app.core.platform.hexToPalette

private const val SecondsPerDay: Long = 86_400L

internal fun List<AiringScheduleDto>.toCalendarFeed(nowEpoch: Long): CalendarFeed {
    val nowMidnight = localMidnightEpoch(nowEpoch)
    val grouped = filter { it.media != null }.groupBy { localMidnightEpoch(it.airingAt) }

    val days = grouped.entries.sortedBy { it.key }.map { entry ->
        val dayMidnight = entry.key
        val schedules = entry.value
        val localDate = (dayMidnight + SecondsPerDay / 2).toLocalMediaReleaseDateTime()
        CalendarDay(
            dayBucket = dayMidnight,
            isoDayOfWeek = isoDayOfWeek(dayMidnight + SecondsPerDay / 2),
            offsetFromToday = ((dayMidnight - nowMidnight) / SecondsPerDay).toInt(),
            day = localDate.day,
            month = localDate.month,
            year = localDate.year,
            items = schedules.map { CalendarEntry(scheduleId = it.id, item = it.toReleaseItem()) },
        )
    }
    return CalendarFeed(days = days)
}

private fun AiringScheduleDto.toReleaseItem(): PlatformHomeReleaseItem {
    val media = media!!
    val hasUserScore = (media.mediaListEntry?.score ?: 0.0) > 0.0
    val totalEpisodes = media.episodes?.takeIf { it > 0 }
    val releaseLabel = totalEpisodes?.let { "Ep $episode / $it" } ?: "Ep $episode"
    return PlatformHomeReleaseItem(
        title = media.title.bestTitle(),
        release = releaseLabel,
        time = formatAiringTime(airingAt),
        colors = hexToPalette(media.coverImage?.color),
        id = media.id,
        coverUrl = media.coverImage?.bestUrl,
        averageScore = media.averageScore,
        favourites = media.favourites,
        popularity = media.popularity,
        isUserFavorited = media.isFavourite ?: false,
        isUserRanked = hasUserScore,
        userStatus = media.mediaListEntry?.status,
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

private fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

private fun formatAiringTime(airingAtEpoch: Long): String {
    val localDateTime = airingAtEpoch.toLocalMediaReleaseDateTime()
    return "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}
