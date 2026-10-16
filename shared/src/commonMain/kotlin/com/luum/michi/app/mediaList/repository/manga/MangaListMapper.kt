package com.luum.michi.app.mediaList.repository.manga

import com.luum.michi.app.mediaList.repository.MediaListEntryDto
import com.luum.michi.app.core.media.repository.allTitles
import com.luum.michi.app.core.media.repository.bestTitle
import com.luum.michi.app.mediaList.repository.toComparableInt
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.toMangaSection
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.parseMediaListStatus
import com.luum.michi.app.mediaList.domain.manga.model.isVolumeBased
import com.luum.michi.app.core.media.domain.MediaFormat
import com.luum.michi.app.core.media.domain.parseMediaFormat
import com.luum.michi.app.core.media.domain.parseMediaSeason
import com.luum.michi.app.core.media.domain.parseMediaWorkStatus
import com.luum.michi.app.core.media.repository.toCoverSizes

internal fun MediaListEntryDto.toMangaListEntry(
    index: Int = 0,
    splitCompleted: Boolean = true,
): MangaListEntry {
    val format = parseMediaFormat(media.format)
    return MangaListEntry(
        id = media.id,
        title = media.title.bestTitle(),
        titles = media.title.allTitles(),
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
): MangaListSection =
    // Unknown/missing status keeps the old fallback (CURRENT).
    (parseMediaListStatus(status?.uppercase()) ?: MediaListStatus.CURRENT)
        .toMangaSection(format, splitCompleted)

