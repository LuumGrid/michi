package com.luum.michi.app.manga.data

import com.luum.michi.app.core.model.toMediaReleaseDateTime
import com.luum.michi.app.core.anilist.dto.MediaDto
import com.luum.michi.app.core.anilist.dto.MediaListEntryDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.anilist.dto.toComparableInt
import com.luum.michi.app.core.model.MediaReleaseDateTime
import com.luum.michi.app.core.platform.hexToPalette
import com.luum.michi.app.manga.presentation.model.MangaListEntry
import com.luum.michi.app.manga.presentation.model.MangaListSection

internal fun MediaListEntryDto.toMangaListEntry(index: Int = 0): MangaListEntry {
    return MangaListEntry(
        id = media.id,
        title = media.title.bestTitle(),
        format = formatLabel(media),
        status = mapMangaStatus(status),
        chaptersProgress = progress,
        totalChapters = media.chapters,
        volumesProgress = progressVolumes ?: 0,
        totalVolumes = media.volumes,
        score = formatScore(score),
        nextChapterRelease = media.nextAiringEpisode.toMediaReleaseDateTime(),
        palette = hexToPalette(media.coverImage?.color),
        coverUrl = media.coverImage?.thumbnailUrl,
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

private fun mapMangaStatus(status: String?): MangaListSection = when (status?.uppercase()) {
    "CURRENT" -> MangaListSection.CURRENT
    "COMPLETED" -> MangaListSection.COMPLETED
    "PAUSED" -> MangaListSection.PAUSED
    "DROPPED" -> MangaListSection.DROPPED
    "PLANNING" -> MangaListSection.PLANNING
    "REPEATING" -> MangaListSection.REPEATING
    else -> MangaListSection.CURRENT
}

private fun formatLabel(media: MediaDto): String {
    return media.format?.replace("_", " ")?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
        ?: "Manga"
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

