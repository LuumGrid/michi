package com.luum.michi.app.dashboard.data

import com.luum.michi.app.core.anilist.dto.DashboardResponseDto
import com.luum.michi.app.core.anilist.dto.MediaDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.platform.components.PlatformHomeMediaItem
import com.luum.michi.app.core.platform.hexToPalette

internal fun DashboardResponseDto.toDashboardFeed(): DashboardFeed = DashboardFeed(
    trendingAnimation = trendingAnime?.media
        ?.map { it.toMediaItem(metaFor = ::animeMeta) }
        .orEmpty(),
    trendingReading = trendingManga?.media
        ?.map { it.toMediaItem(metaFor = ::mangaMeta) }
        .orEmpty(),
    thisSeason = popularThisSeason?.media
        ?.map { it.toMediaItem(metaFor = ::animeMeta) }
        .orEmpty(),
    upcomingNextSeason = upcomingNextSeason?.media
        ?.map { it.toMediaItem(metaFor = ::animeMeta) }
        .orEmpty(),
    allTimePopularAnime = allTimePopularAnime?.media
        ?.map { it.toMediaItem(metaFor = ::animeMeta) }
        .orEmpty(),
    allTimePopularManga = allTimePopularManga?.media
        ?.map { it.toMediaItem(metaFor = ::mangaMeta) }
        .orEmpty(),
    topAnime = topAnime?.media
        ?.map { it.toMediaItem(metaFor = ::animeMeta) }
        .orEmpty(),
    topManga = topManga?.media
        ?.map { it.toMediaItem(metaFor = ::mangaMeta) }
        .orEmpty(),
)

private fun MediaDto.toMediaItem(metaFor: (MediaDto) -> String): PlatformHomeMediaItem =
    PlatformHomeMediaItem(
        title = title.bestTitle(),
        meta = metaFor(this),
        colors = hexToPalette(coverImage?.color),
        id = id,
        coverUrl = coverImage?.thumbnailUrl,
        averageScore = averageScore,
        favourites = favourites,
        isUserFavorited = isFavourite == true,
        isUserRanked = mediaListEntry != null && (mediaListEntry.score ?: 0.0) > 0.0,
    )

private fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

private fun animeMeta(media: MediaDto): String {
    val format = formatLabel(media.format) ?: "Anime"
    val episodes = media.episodes?.takeIf { it > 0 }?.let { " · $it ep" }.orEmpty()
    return format + episodes
}

private fun mangaMeta(media: MediaDto): String {
    val format = formatLabel(media.format) ?: "Manga"
    val chapters = media.chapters?.takeIf { it > 0 }?.let { " · $it ch" }.orEmpty()
    return format + chapters
}

private fun formatLabel(format: String?): String? = format
    ?.replace("_", " ")
    ?.lowercase()
    ?.replaceFirstChar { it.uppercase() }

