package com.luum.michi.app.mediaList.data.anime

import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.AnimeMediaFormat
import com.luum.michi.app.mediaList.domain.anime.model.completedSection
import com.luum.michi.app.mediaList.domain.anime.model.parseAnimeMediaFormat
import com.luum.michi.app.core.anilist.dto.MediaListEntryDto
import com.luum.michi.app.core.anilist.dto.toComparableInt
import com.luum.michi.app.core.model.toMediaReleaseDateTime
import com.luum.michi.app.core.platform.hexToPalette

internal fun MediaListEntryDto.toAnimeListEntry(index: Int = 0): AnimeListEntry {
    val section = mapAnimeStatus(status, parseAnimeMediaFormat(media.format))
    return AnimeListEntry(
        id = media.id,
        title = media.title.bestTitle(),
        format = parseAnimeMediaFormat(media.format),
        status = section,
        progress = progress,
        totalEpisodes = media.episodes,
        score = score,
        nextEpisodeRelease = media.nextAiringEpisode.toMediaReleaseDateTime(),
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
        nextEpisodeNumber = media.nextAiringEpisode?.episode,
    )
}

private fun mapAnimeStatus(status: String?, format: AnimeMediaFormat): AnimeListSection {
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

private fun com.luum.michi.app.core.anilist.dto.MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}