package com.luum.michi.app.mediaList.repository.anime

import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.toAnimeSection
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.parseMediaListStatus
import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.parseMediaFormat
import com.luum.michi.app.core.model.parseMediaSeason
import com.luum.michi.app.core.model.parseMediaWorkStatus
import com.luum.michi.app.mediaList.repository.MediaListEntryDto
import com.luum.michi.app.core.network.repository.dto.allTitles
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.mediaList.repository.toComparableInt
import com.luum.michi.app.core.network.repository.toMediaReleaseDateTime
import com.luum.michi.app.core.network.repository.toCoverSizes

internal fun MediaListEntryDto.toAnimeListEntry(
    index: Int = 0,
    splitCompleted: Boolean = true,
): AnimeListEntry {
    val section = mapAnimeStatus(status, parseMediaFormat(media.format), splitCompleted)
    return AnimeListEntry(
        id = media.id,
        title = media.title.bestTitle(),
        titles = media.title.allTitles(),
        format = parseMediaFormat(media.format),
        mediaStatus = parseMediaWorkStatus(media.status),
        status = section,
        progress = progress,
        totalEpisodes = media.episodes,
        score = score,
        nextEpisodeRelease = media.nextAiringEpisode.toMediaReleaseDateTime(),
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
        nextAiringAt = media.nextAiringEpisode?.airingAt ?: 0L,
        nextEpisodeNumber = media.nextAiringEpisode?.episode,
        genres = media.genres.orEmpty(),
        isAdult = media.isAdult == true,
        season = parseMediaSeason(media.season),
        seasonYear = media.seasonYear ?: media.startDate?.year,
    )
}

private fun mapAnimeStatus(status: String?, format: MediaFormat, splitCompleted: Boolean): AnimeListSection =
    // Unknown/missing status keeps the old fallback (CURRENT → WATCHING).
    (parseMediaListStatus(status?.uppercase()) ?: MediaListStatus.CURRENT)
        .toAnimeSection(format, splitCompleted)