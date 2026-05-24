package com.luum.michi.app.calendar.data

import com.luum.michi.app.core.anilist.dto.AiringScheduleDto
import com.luum.michi.app.core.anilist.dto.AiringSchedulePageDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.media.isoDayOfWeek
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem
import com.luum.michi.app.core.platform.hexToPalette

private const val SecondsPerDay: Long = 86_400L

internal fun AiringSchedulePageDto.toCalendarFeed(nowEpoch: Long): CalendarFeed {
    val nowBucket = nowEpoch / SecondsPerDay
    val grouped = airingSchedules
        .filter { it.media != null }
        .groupBy { it.airingAt / SecondsPerDay }
        .toSortedMap()

    val days = grouped.entries.map { entry ->
        val dayBucket = entry.key
        val schedules = entry.value
        val firstEpoch = schedules.first().airingAt
        CalendarDay(
            dayBucket = dayBucket,
            isoDayOfWeek = isoDayOfWeek(firstEpoch),
            offsetFromToday = (dayBucket - nowBucket).toInt(),
            items = schedules.map { it.toReleaseItem() },
        )
    }
    return CalendarFeed(days = days)
}

private fun AiringScheduleDto.toReleaseItem(): PlatformHomeReleaseItem {
    val media = media!!
    return PlatformHomeReleaseItem(
        title = media.title.bestTitle(),
        release = "Ep $episode",
        time = formatAiringTime(airingAt),
        colors = hexToPalette(media.coverImage?.color),
        id = media.id,
    )
}

private fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

private fun formatAiringTime(airingAtEpoch: Long): String {
    val secondsInDay = ((airingAtEpoch % SecondsPerDay) + SecondsPerDay) % SecondsPerDay
    val hour = (secondsInDay / 3600L).toInt()
    val minute = ((secondsInDay % 3600L) / 60L).toInt()
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
