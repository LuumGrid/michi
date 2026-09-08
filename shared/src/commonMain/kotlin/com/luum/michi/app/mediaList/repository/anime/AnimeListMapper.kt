package com.luum.michi.app.mediaList.repository.anime

import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.completedSection
import com.luum.michi.app.core.domain.model.MediaFormat
import com.luum.michi.app.core.domain.model.parseMediaFormat
import com.luum.michi.app.core.domain.model.parseMediaSeason
import com.luum.michi.app.core.repository.anilist.dto.MediaListEntryDto
import com.luum.michi.app.core.repository.anilist.dto.bestTitle
import com.luum.michi.app.core.repository.anilist.dto.toComparableInt
import com.luum.michi.app.core.repository.anilist.toMediaReleaseDateTime

internal fun MediaListEntryDto.toAnimeListEntry(index: Int = 0): AnimeListEntry {
    val section = mapAnimeStatus(status, parseMediaFormat(media.format))
    return AnimeListEntry(
        id = media.id,
        title = media.title.bestTitle(),
        format = parseMediaFormat(media.format),
        status = section,
        progress = progress,
        totalEpisodes = media.episodes,
        score = score,
        nextEpisodeRelease = media.nextAiringEpisode.toMediaReleaseDateTime(),
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
        nextAiringAt = media.nextAiringEpisode?.airingAt ?: 0L,
        nextEpisodeNumber = media.nextAiringEpisode?.episode,
        genres = media.genres.orEmpty(),
        season = parseMediaSeason(media.season),
        seasonYear = media.seasonYear ?: media.startDate?.year,
    )
}

private fun mapAnimeStatus(status: String?, format: MediaFormat): AnimeListSection {
    return when (status?.uppercase()) {
        "CURRENT" -> AnimeListSection.WATCHING
        "COMPLETED" -> format.completedSection()
        "PAUSED" -> AnimeListSection.PAUSED
        "DROPPED" -> AnimeListSection.DROPPED
        "PLANNING" -> AnimeListSection.PLANNING
        "REPEATING" -> AnimeListSection.REWATCHING
        else -> AnimeListSection.WATCHING
    }
}