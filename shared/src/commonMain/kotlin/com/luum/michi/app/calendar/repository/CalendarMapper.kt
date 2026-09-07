package com.luum.michi.app.calendar.repository

import com.luum.michi.app.calendar.domain.CalendarDay
import com.luum.michi.app.calendar.domain.CalendarEntry
import com.luum.michi.app.calendar.domain.CalendarFeed
import com.luum.michi.app.core.repository.anilist.dto.AiringScheduleDto
import com.luum.michi.app.core.repository.anilist.dto.MediaExternalLinkDto
import com.luum.michi.app.core.repository.anilist.dto.bestTitle
import com.luum.michi.app.core.domain.model.isoDayOfWeek
import com.luum.michi.app.core.domain.model.localMidnightEpoch
import com.luum.michi.app.core.domain.model.toLocalMediaReleaseDateTime
import com.luum.michi.app.calendar.domain.model.ReleaseItem
import com.luum.michi.app.calendar.domain.model.StreamingPlatform

private const val SecondsPerDay: Long = 86_400L

internal fun List<AiringScheduleDto>.toCalendarFeed(nowEpoch: Long): CalendarFeed {
    val nowMidnight = localMidnightEpoch(nowEpoch)
    val grouped = groupBy { localMidnightEpoch(it.airingAt) }

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
            items = schedules.mapNotNull { schedule ->
                schedule.toReleaseItemOrNull()?.let { item ->
                    CalendarEntry(scheduleId = schedule.id, item = item)
                }
            },
        )
    }
    return CalendarFeed(days = days)
}

private fun AiringScheduleDto.toReleaseItemOrNull(): ReleaseItem? {
    val media = media ?: return null
    val hasUserScore = (media.mediaListEntry?.score ?: 0.0) > 0.0
    val totalEpisodes = media.episodes?.takeIf { it > 0 }
    val releaseLabel = totalEpisodes?.let { "Ep $episode / $it" } ?: "Ep $episode"
    return ReleaseItem(
        title = media.title.bestTitle(),
        release = releaseLabel,
        time = formatAiringTime(airingAt),
        paletteHex = media.coverImage?.color,
        id = media.id,
        coverUrl = media.coverImage?.thumbnailUrl,
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

private fun formatAiringTime(airingAtEpoch: Long): String {
    val localDateTime = airingAtEpoch.toLocalMediaReleaseDateTime()
    return "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}
