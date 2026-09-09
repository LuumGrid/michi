package com.luum.michi.app.calendar.repository

import com.luum.michi.app.calendar.domain.CalendarDay
import com.luum.michi.app.calendar.domain.CalendarEntry
import com.luum.michi.app.calendar.domain.CalendarFeed
import com.luum.michi.app.core.network.repository.dto.AiringScheduleDto
import com.luum.michi.app.core.network.repository.dto.MediaExternalLinkDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.model.localMidnightEpoch
import com.luum.michi.app.core.model.parseMediaSeason
import com.luum.michi.app.core.model.toLocalMediaReleaseDateTime
import com.luum.michi.app.calendar.domain.model.ReleaseItem
import com.luum.michi.app.calendar.domain.model.StreamingPlatform
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import com.luum.michi.app.core.medialist.domain.parseMediaListStatus

internal fun List<AiringScheduleDto>.toCalendarFeed(
    nowEpoch: Long,
    timeZoneProvider: () -> TimeZone = { TimeZone.currentSystemDefault() },
): CalendarFeed {
    val zone = timeZoneProvider()
    fun localDateOf(epochSeconds: Long): LocalDate =
        Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(zone).date
    val grouped = groupBy { localDateOf(it.airingAt) }

    val days = grouped.entries.sortedBy { it.key }.map { (date, schedules) ->
        val dayMidnight = date.atStartOfDayIn(zone).epochSeconds
        CalendarDay(
            dayBucket = dayMidnight,
            isoDayOfWeek = date.dayOfWeek.isoDayNumber,
            // Whole calendar days between the two midnights in zone: exact across
            // DST transitions, where wall-clock days are 23/25h long.
            offsetFromToday = Instant.fromEpochSeconds(localMidnightEpoch(nowEpoch, zone))
                .daysUntil(Instant.fromEpochSeconds(dayMidnight), zone),
            day = date.day,
            month = date.month.number,
            year = date.year,
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
