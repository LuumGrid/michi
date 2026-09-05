package com.luum.michi.app.manga.data

import com.luum.michi.app.core.model.toMediaReleaseDateTime
import com.luum.michi.app.core.anilist.dto.MediaListEntryDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.anilist.dto.toComparableInt
import com.luum.michi.app.core.platform.hexToPalette
import com.luum.michi.app.manga.domain.model.MangaListEntry
import com.luum.michi.app.manga.domain.model.MangaListSection
import com.luum.michi.app.manga.domain.model.isVolumeBased
import com.luum.michi.app.manga.domain.model.parseMangaMediaFormat

internal fun MediaListEntryDto.toMangaListEntry(index: Int = 0): MangaListEntry {
    val format = parseMangaMediaFormat(media.format)
    return MangaListEntry(
        id = media.id,
        title = media.title.bestTitle(),
        format = format,
        status = mapMangaStatus(status),
        chaptersProgress = progress,
        totalChapters = media.chapters,
        volumesProgress = progressVolumes ?: 0,
        totalVolumes = media.volumes,
        tracksByVolume = format.isVolumeBased() || media.chapters == 0,
        score = score,
        nextChapterRelease = media.nextAiringEpisode.toMediaReleaseDateTime(),
        palette = hexToPalette(media.coverImage?.color),
        coverUrl = media.coverImage?.thumbnailUrl,
        originalIndex = index,
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

private fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

