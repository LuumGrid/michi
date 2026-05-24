package com.luum.michi.app.reading.data

import com.luum.michi.app.animation.data.toMediaReleaseDateTime
import com.luum.michi.app.core.anilist.dto.MediaDto
import com.luum.michi.app.core.anilist.dto.MediaListEntryDto
import com.luum.michi.app.core.anilist.dto.MediaNextAiringEpisodeDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.anilist.dto.toComparableInt
import com.luum.michi.app.core.model.MediaReleaseDateTime
import com.luum.michi.app.core.platform.hexToPalette
import com.luum.michi.app.reading.presentation.model.ReadingListEntry
import com.luum.michi.app.reading.presentation.model.ReadingListSection

internal fun MediaListEntryDto.toReadingListEntry(index: Int = 0): ReadingListEntry {
    return ReadingListEntry(
        id = media.id,
        title = media.title.bestTitle(),
        format = formatLabel(media),
        status = mapReadingStatus(status),
        chaptersProgress = progress,
        totalChapters = media.chapters,
        volumesProgress = progressVolumes ?: 0,
        totalVolumes = media.volumes,
        score = formatScore(score),
        nextChapterRelease = media.nextAiringEpisode.toMediaReleaseDateTime(),
        palette = hexToPalette(media.coverImage?.color),
        coverUrl = media.coverImage?.extraLarge ?: media.coverImage?.large,
        originalIndex = index,
        scoreDouble = score,
        updatedAt = updatedAt ?: 0L,
        startedAtInt = startedAt.toComparableInt(),
        completedAtInt = completedAt.toComparableInt(),
        releaseDateInt = media.startDate.toComparableInt(),
        averageScore = media.averageScore ?: 0,
        popularity = media.popularity ?: 0,
        favouritesCount = media.favourites ?: 0,
        trending = media.trending ?: 0,
        priority = priority ?: 0,
        nextAiringAt = media.nextAiringEpisode?.airingAt ?: 0L,
    )
}

private fun mapReadingStatus(status: String?): ReadingListSection = when (status?.uppercase()) {
    "CURRENT" -> ReadingListSection.READING
    "COMPLETED" -> ReadingListSection.COMPLETED
    "PAUSED" -> ReadingListSection.PAUSED
    "DROPPED" -> ReadingListSection.DROPPED
    "PLANNING" -> ReadingListSection.PLANNING
    "REPEATING" -> ReadingListSection.REREADING
    else -> ReadingListSection.READING
}

private fun formatLabel(media: MediaDto): String {
    return media.format?.replace("_", " ")?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
        ?: "Reading"
}

private fun formatScore(score: Double): String {
    if (score <= 0.0) return "-"
    if (score == score.toLong().toDouble()) return score.toLong().toString()
    return ((score * 10).toLong() / 10.0).toString()
}

private fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

private fun MediaNextAiringEpisodeDto?.toMediaReleaseDateTime(): MediaReleaseDateTime? {
    if (this == null) return null
    return airingAt.toMediaReleaseDateTime()
}
