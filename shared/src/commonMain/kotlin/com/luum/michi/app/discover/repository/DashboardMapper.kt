package com.luum.michi.app.discover.repository

import com.luum.michi.app.core.network.repository.dto.DashboardResponseDto
import com.luum.michi.app.core.network.repository.dto.MediaDto
import com.luum.michi.app.core.network.repository.dto.MediaPageDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.network.repository.toCoverSizes
import com.luum.michi.app.core.model.label
import com.luum.michi.app.core.model.parseMediaFormat
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.MediaItem
import com.luum.michi.app.discover.domain.DashboardFeed

/** Maps each present alias to its rail; absent aliases (other type) are skipped. */
internal fun DashboardResponseDto.toDashboardFeed(strings: LanguageStrings): DashboardFeed {
    val sections = mutableMapOf<DashboardSectionKind, List<MediaItem>>()
    sections.putPage(DashboardSectionKind.SEASONAL, seasonal, strings)
    sections.putPage(DashboardSectionKind.UPCOMING_SEASON, upcomingSeason, strings)
    sections.putPage(DashboardSectionKind.TRENDING_ANIME, trendingAnime, strings)
    sections.putPage(DashboardSectionKind.POPULAR_ANIME, popularAnime, strings)
    sections.putPage(DashboardSectionKind.POPULAR_MOVIE, popularMovie, strings)
    sections.putPage(DashboardSectionKind.BEST_ANIME, bestAnime, strings)
    sections.putPage(DashboardSectionKind.BEST_MOVIE, bestMovie, strings)
    sections.putPage(DashboardSectionKind.BEST_ANIME_NOW, bestAnimeNow, strings)
    sections.putPage(DashboardSectionKind.BEST_MOVIE_NOW, bestMovieNow, strings)
    sections.putPage(DashboardSectionKind.NEW_RELEASES_MANGA, newReleasesManga, strings)
    sections.putPage(DashboardSectionKind.TRENDING_MANGA, trendingManga, strings)
    sections.putPage(DashboardSectionKind.POPULAR_MANGA, popularManga, strings)
    sections.putPage(DashboardSectionKind.POPULAR_LIGHT_NOVEL, popularLightNovel, strings)
    sections.putPage(DashboardSectionKind.POPULAR_MANHWA, popularManhwa, strings)
    sections.putPage(DashboardSectionKind.POPULAR_MANHUA, popularManhua, strings)
    sections.putPage(DashboardSectionKind.BEST_MANGA, bestManga, strings)
    sections.putPage(DashboardSectionKind.BEST_LIGHT_NOVEL, bestLightNovel, strings)
    sections.putPage(DashboardSectionKind.BEST_MANHWA, bestManhwa, strings)
    sections.putPage(DashboardSectionKind.BEST_MANHUA, bestManhua, strings)
    return DashboardFeed(sections)
}

private fun MutableMap<DashboardSectionKind, List<MediaItem>>.putPage(
    kind: DashboardSectionKind,
    page: MediaPageDto?,
    strings: LanguageStrings,
) {
    val media = page?.media ?: return
    if (media.isEmpty()) return
    val isManga = kind.mediaType == DashboardMediaType.MANGA
    put(kind, media.map { it.toMediaItem(strings, isManga) })
}

private fun MediaDto.toMediaItem(strings: LanguageStrings, isManga: Boolean): MediaItem =
    MediaItem(
        title = title.bestTitle(),
        meta = if (isManga) mangaMeta(this, strings) else animeMeta(this, strings),
        id = id,
        cover = coverImage?.toCoverSizes(),
        placeholderHex = coverImage?.color,
        averageScore = averageScore,
        popularity = popularity,
        favourites = favourites,
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
