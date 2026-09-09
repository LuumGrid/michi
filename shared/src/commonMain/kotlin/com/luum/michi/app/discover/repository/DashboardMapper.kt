package com.luum.michi.app.discover.repository

import com.luum.michi.app.core.network.repository.dto.DashboardResponseDto
import com.luum.michi.app.core.network.repository.dto.MediaDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.model.label
import com.luum.michi.app.core.model.parseMediaFormat
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.discover.domain.model.MediaItem
import com.luum.michi.app.discover.domain.DashboardFeed

internal fun DashboardResponseDto.toDashboardFeed(strings: LanguageStrings): DashboardFeed = DashboardFeed(
    trendingAnime = trendingAnime?.media
        ?.map { it.toMediaItem(metaFor = { m -> animeMeta(m, strings) }) }
        .orEmpty(),
    trendingManga = trendingManga?.media
        ?.map { it.toMediaItem(metaFor = { m -> mangaMeta(m, strings) }) }
        .orEmpty(),
    thisSeason = popularThisSeason?.media
        ?.map { it.toMediaItem(metaFor = { m -> animeMeta(m, strings) }) }
        .orEmpty(),
    upcomingNextSeason = upcomingNextSeason?.media
        ?.map { it.toMediaItem(metaFor = { m -> animeMeta(m, strings) }) }
        .orEmpty(),
    allTimePopularAnime = allTimePopularAnime?.media
        ?.map { it.toMediaItem(metaFor = { m -> animeMeta(m, strings) }) }
        .orEmpty(),
    allTimePopularManga = allTimePopularManga?.media
        ?.map { it.toMediaItem(metaFor = { m -> mangaMeta(m, strings) }) }
        .orEmpty(),
    topAnime = topAnime?.media
        ?.map { it.toMediaItem(metaFor = { m -> animeMeta(m, strings) }) }
        .orEmpty(),
    topManga = topManga?.media
        ?.map { it.toMediaItem(metaFor = { m -> mangaMeta(m, strings) }) }
        .orEmpty(),
)

private fun MediaDto.toMediaItem(metaFor: (MediaDto) -> String): MediaItem =
    MediaItem(
        title = title.bestTitle(),
        meta = metaFor(this),
        paletteHex = coverImage?.color,
        id = id,
        coverUrl = coverImage?.thumbnailUrl,
        averageScore = averageScore,
        favourites = favourites,
        isUserFavorited = isFavourite == true,
        isUserRanked = mediaListEntry != null && (mediaListEntry.score ?: 0.0) > 0.0,
    )

private fun animeMeta(media: MediaDto, strings: LanguageStrings): String {
    val format = parseMediaFormat(media.format).label(unknownFallback = "Anime")
    val episodes = media.episodes?.takeIf { it > 0 }?.let { " · ${strings.exploreEpisodeCountLabel(it)}" }.orEmpty()
    return format + episodes
}

private fun mangaMeta(media: MediaDto, strings: LanguageStrings): String {
    val format = parseMediaFormat(media.format).label(unknownFallback = "Manga")
    val chapters = media.chapters?.takeIf { it > 0 }?.let { " · ${strings.exploreChapterCountLabel(it)}" }.orEmpty()
    return format + chapters
}

