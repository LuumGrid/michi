package com.luum.michi.app.mediaList.repository.manga

import com.luum.michi.app.core.network.repository.dto.MediaListEntryDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.network.repository.dto.toComparableInt
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.completedMangaSection
import com.luum.michi.app.mediaList.domain.manga.model.isVolumeBased
import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.parseMediaFormat
import com.luum.michi.app.core.model.parseMediaSeason
import com.luum.michi.app.core.model.parseMediaWorkStatus
import com.luum.michi.app.core.network.repository.toCoverSizes

internal fun MediaListEntryDto.toMangaListEntry(
    index: Int = 0,
    splitCompleted: Boolean = true,
): MangaListEntry {
    val format = parseMediaFormat(media.format)
    return MangaListEntry(
        id = media.id,
        title = media.title.bestTitle(),
        titles = listOfNotNull(
            media.title?.romaji?.takeIf { it.isNotBlank() },
            media.title?.english?.takeIf { it.isNotBlank() },
            media.title?.native?.takeIf { it.isNotBlank() },
        ).distinct(),
        format = format,
        mediaStatus = parseMediaWorkStatus(media.status),
        status = mapMangaStatus(status, format, splitCompleted),
        chaptersProgress = progress,
        totalChapters = media.chapters,
        volumesProgress = progressVolumes ?: 0,
        totalVolumes = media.volumes,
        tracksByVolume = format.isVolumeBased() || media.chapters == 0,
        score = score,
        paletteHex = media.coverImage?.color,
        cover = media.coverImage?.toCoverSizes(),
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
        isAdult = media.isAdult == true,
        season = parseMediaSeason(media.season),
        seasonYear = media.seasonYear ?: media.startDate?.year,
    )
}

private fun mapMangaStatus(
    status: String?,
    format: MediaFormat,
    splitCompleted: Boolean,
): MangaListSection = when (status?.uppercase()) {
    "CURRENT" -> MangaListSection.CURRENT
    "COMPLETED" -> format.completedMangaSection(splitCompleted)
    "PAUSED" -> MangaListSection.PAUSED
    "DROPPED" -> MangaListSection.DROPPED
    "PLANNING" -> MangaListSection.PLANNING
    "REPEATING" -> MangaListSection.REPEATING
    else -> MangaListSection.CURRENT
}

