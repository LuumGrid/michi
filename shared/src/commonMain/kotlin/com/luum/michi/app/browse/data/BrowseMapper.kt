package com.luum.michi.app.browse.data

import com.luum.michi.app.core.anilist.dto.BrowseResponseDto
import com.luum.michi.app.core.anilist.dto.MediaDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.platform.components.PlatformHomeMediaItem
import com.luum.michi.app.core.platform.hexToPalette

internal fun BrowseResponseDto.toBrowseFeed(): BrowseFeed = BrowseFeed(
    popularThisSeason = popularThisSeason?.media?.map { it.toAnimeItem() }.orEmpty(),
    upcomingNextSeason = upcomingNextSeason?.media?.map { it.toAnimeItem() }.orEmpty(),
    allTimePopularAnime = allTimePopularAnime?.media?.map { it.toAnimeItem() }.orEmpty(),
    allTimePopularManga = allTimePopularManga?.media?.map { it.toMangaItem() }.orEmpty(),
    topAnime = topAnime?.media?.map { it.toAnimeItem() }.orEmpty(),
    topManga = topManga?.media?.map { it.toMangaItem() }.orEmpty(),
)

private fun MediaDto.toAnimeItem(): PlatformHomeMediaItem = PlatformHomeMediaItem(
    title = title.bestTitle(),
    meta = animeMeta(this),
    colors = hexToPalette(coverImage?.color),
    id = id,
)

private fun MediaDto.toMangaItem(): PlatformHomeMediaItem = PlatformHomeMediaItem(
    title = title.bestTitle(),
    meta = mangaMeta(this),
    colors = hexToPalette(coverImage?.color),
    id = id,
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
