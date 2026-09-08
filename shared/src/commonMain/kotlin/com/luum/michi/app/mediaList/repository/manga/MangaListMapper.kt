package com.luum.michi.app.mediaList.repository.manga

import com.luum.michi.app.core.repository.anilist.dto.MediaListEntryDto
import com.luum.michi.app.core.repository.anilist.dto.bestTitle
import com.luum.michi.app.core.repository.anilist.dto.toComparableInt
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.isVolumeBased
import com.luum.michi.app.core.domain.model.parseMediaFormat
import com.luum.michi.app.core.domain.model.parseMediaSeason

internal fun MediaListEntryDto.toMangaListEntry(index: Int = 0): MangaListEntry {
    val format = parseMediaFormat(media.format)
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
        paletteHex = media.coverImage?.color,
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
        genres = media.genres.orEmpty(),
        season = parseMediaSeason(media.season),
        seasonYear = media.seasonYear ?: media.startDate?.year,
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

