package com.luum.michi.app.discover.domain.model

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.MediaSeasonYear
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.media.domain.currentSeasonAndYear
import com.luum.michi.app.core.media.domain.label
import com.luum.michi.app.core.media.domain.next
import com.luum.michi.app.core.media.domain.previous
import com.luum.michi.app.core.medialist.domain.UserListSort

/**
 * One dashboard rail. Each kind belongs to a media type and maps 1:1 to a
 * GraphQL alias in the matching dashboard query (`DashboardResponseDto`), so
 * the enum order is the on-screen rail order.
 */
internal enum class DashboardSectionKind(val mediaType: DashboardMediaType) {
    // Anime
    SEASONAL(DashboardMediaType.ANIME),
    UPCOMING_SEASON(DashboardMediaType.ANIME),
    TRENDING_ANIME(DashboardMediaType.ANIME),
    POPULAR_ANIME(DashboardMediaType.ANIME),
    POPULAR_MOVIE(DashboardMediaType.ANIME),
    BEST_ANIME(DashboardMediaType.ANIME),
    BEST_MOVIE(DashboardMediaType.ANIME),
    BEST_ANIME_NOW(DashboardMediaType.ANIME),
    BEST_MOVIE_NOW(DashboardMediaType.ANIME),

    // Manga
    NEW_RELEASES_MANGA(DashboardMediaType.MANGA),
    TRENDING_MANGA(DashboardMediaType.MANGA),
    POPULAR_MANGA(DashboardMediaType.MANGA),
    POPULAR_LIGHT_NOVEL(DashboardMediaType.MANGA),
    POPULAR_MANHWA(DashboardMediaType.MANGA),
    POPULAR_MANHUA(DashboardMediaType.MANGA),
    BEST_MANGA(DashboardMediaType.MANGA),
    BEST_LIGHT_NOVEL(DashboardMediaType.MANGA),
    BEST_MANHWA(DashboardMediaType.MANGA),
    BEST_MANHUA(DashboardMediaType.MANGA),
}

/** Resolves a `DetailDestination.Catalog` kind id (the enum name) back to its enum. */
internal fun catalogKindOf(kindId: String): DashboardSectionKind? =
    DashboardSectionKind.entries.firstOrNull { it.name == kindId }

/** One badge a dashboard card may show, in paint order (first = top-start). */
internal enum class DashboardCardStat { SCORE, POPULARITY }

/**
 * Which badges a rail's cards show, in order (max two). "Most popular" shelves
 * lead with the popularity they are sorted by; "best rated" ones and the rest
 * lead with the score. Single source of truth for the rail badges (and the
 * catalog grid's, through the same list).
 */
internal fun DashboardSectionKind.cardStats(): List<DashboardCardStat> = when (this) {
    DashboardSectionKind.POPULAR_ANIME,
    DashboardSectionKind.POPULAR_MOVIE,
    DashboardSectionKind.POPULAR_MANGA,
    DashboardSectionKind.POPULAR_LIGHT_NOVEL,
    DashboardSectionKind.POPULAR_MANHWA,
    DashboardSectionKind.POPULAR_MANHUA ->
        listOf(DashboardCardStat.POPULARITY, DashboardCardStat.SCORE)
    DashboardSectionKind.SEASONAL,
    DashboardSectionKind.UPCOMING_SEASON,
    DashboardSectionKind.TRENDING_ANIME,
    DashboardSectionKind.BEST_ANIME,
    DashboardSectionKind.BEST_MOVIE,
    DashboardSectionKind.BEST_ANIME_NOW,
    DashboardSectionKind.BEST_MOVIE_NOW,
    DashboardSectionKind.NEW_RELEASES_MANGA,
    DashboardSectionKind.TRENDING_MANGA,
    DashboardSectionKind.BEST_MANGA,
    DashboardSectionKind.BEST_LIGHT_NOVEL,
    DashboardSectionKind.BEST_MANHWA,
    DashboardSectionKind.BEST_MANHUA ->
        listOf(DashboardCardStat.SCORE, DashboardCardStat.POPULARITY)
}

/** Rail title; the season rails read "Fall 2026" / "Winter 2027" (localized). */
internal fun DashboardSectionKind.title(
    strings: LanguageStrings,
    current: MediaSeasonYear,
    next: MediaSeasonYear,
): String = when (this) {
    DashboardSectionKind.SEASONAL -> "${current.season.label(strings)} ${current.year}"
    DashboardSectionKind.UPCOMING_SEASON -> "${next.season.label(strings)} ${next.year}"
    DashboardSectionKind.TRENDING_ANIME -> strings.discoverTrendingAnimeTitle
    DashboardSectionKind.POPULAR_ANIME -> strings.discoverAllTimePopularAnimeTitle
    DashboardSectionKind.POPULAR_MOVIE -> strings.discoverAllTimePopularMovieTitle
    DashboardSectionKind.BEST_ANIME -> strings.discoverBestRatedAnimeTitle
    DashboardSectionKind.BEST_MOVIE -> strings.discoverBestRatedMovieTitle
    DashboardSectionKind.BEST_ANIME_NOW -> strings.discoverBestRatedAnimeNowTitle
    DashboardSectionKind.BEST_MOVIE_NOW -> strings.discoverBestRatedMovieNowTitle
    DashboardSectionKind.NEW_RELEASES_MANGA -> strings.discoverNewReleasesMangaTitle
    DashboardSectionKind.TRENDING_MANGA -> strings.discoverTrendingMangaTitle
    DashboardSectionKind.POPULAR_MANGA -> strings.discoverAllTimePopularMangaTitle
    DashboardSectionKind.POPULAR_LIGHT_NOVEL -> strings.discoverAllTimePopularLightNovelTitle
    DashboardSectionKind.POPULAR_MANHWA -> strings.discoverAllTimePopularManhwaTitle
    DashboardSectionKind.POPULAR_MANHUA -> strings.discoverAllTimePopularManhuaTitle
    DashboardSectionKind.BEST_MANGA -> strings.discoverBestRatedMangaTitle
    DashboardSectionKind.BEST_LIGHT_NOVEL -> strings.discoverBestRatedLightNovelTitle
    DashboardSectionKind.BEST_MANHWA -> strings.discoverBestRatedManhwaTitle
    DashboardSectionKind.BEST_MANHUA -> strings.discoverBestRatedManhuaTitle
}

/**
 * The 4 seasons a season catalog's chip rail offers, in chronological order:
 * the three before the current season plus the current one (SEASONAL, current
 * selected last), or the next season plus the three after it (UPCOMING_SEASON,
 * next selected first). Null for every other rail, which has no selector.
 */
internal fun DashboardSectionKind.seasonRail(
    now: MediaSeasonYear = currentSeasonAndYear(),
): List<MediaSeasonYear>? = when (this) {
    DashboardSectionKind.SEASONAL -> listOf(now.rewind(3), now.rewind(2), now.rewind(1), now)
    DashboardSectionKind.UPCOMING_SEASON -> {
        val base = now.next()
        listOf(base, base.advance(1), base.advance(2), base.advance(3))
    }
    else -> null
}

/** The season a season rail highlights before the user taps a chip. */
internal fun List<MediaSeasonYear>.defaultSeason(kind: DashboardSectionKind): MediaSeasonYear =
    if (kind == DashboardSectionKind.UPCOMING_SEASON) first() else last()

private fun MediaSeasonYear.rewind(times: Int): MediaSeasonYear =
    (0 until times).fold(this) { acc, _ -> acc.previous() }

private fun MediaSeasonYear.advance(times: Int): MediaSeasonYear =
    (0 until times).fold(this) { acc, _ -> acc.next() }

private val AnimeOrMovieFormats = listOf("TV", "TV_SHORT", "OVA", "ONA", "SPECIAL")
private val MovieFormats = listOf("MOVIE")
private val MangaFormats = listOf("MANGA", "ONE_SHOT")
private val LightNovelFormats = listOf("NOVEL")

/**
 * The exact catalog criteria of this rail's alias in `DashboardRepositoryImpl`,
 * so "View all" opens the same shelf (type, formats, countries, status, season,
 * `startDate_greater`, popularity floor) with the rail's default sort. Seasons
 * are computed at open time; [now]/[recentStart] are injectable for tests.
 */
internal fun DashboardSectionKind.catalogFilter(
    now: MediaSeasonYear = currentSeasonAndYear(),
    recentStart: Int = recentStartFuzzy(),
): MediaCatalogFilter = when (this) {
    DashboardSectionKind.SEASONAL -> MediaCatalogFilter(
        type = MediaType.ANIME,
        season = now.season.name,
        seasonYear = now.year,
        defaultSortOption = UserListSort.POPULARITY,
    )
    DashboardSectionKind.UPCOMING_SEASON -> {
        val next = now.next()
        MediaCatalogFilter(
            type = MediaType.ANIME,
            season = next.season.name,
            seasonYear = next.year,
            defaultSortOption = UserListSort.POPULARITY,
        )
    }
    DashboardSectionKind.TRENDING_ANIME -> MediaCatalogFilter(
        type = MediaType.ANIME,
        defaultSortOption = UserListSort.TRENDING,
    )
    DashboardSectionKind.POPULAR_ANIME -> MediaCatalogFilter(
        type = MediaType.ANIME,
        formats = AnimeOrMovieFormats,
        defaultSortOption = UserListSort.POPULARITY,
    )
    DashboardSectionKind.POPULAR_MOVIE -> MediaCatalogFilter(
        type = MediaType.ANIME,
        formats = MovieFormats,
        defaultSortOption = UserListSort.POPULARITY,
    )
    DashboardSectionKind.BEST_ANIME -> MediaCatalogFilter(
        type = MediaType.ANIME,
        formats = AnimeOrMovieFormats,
        popularityGreater = BestRatedMinPopularity,
        defaultSortOption = UserListSort.AVERAGE_SCORE,
    )
    DashboardSectionKind.BEST_MOVIE -> MediaCatalogFilter(
        type = MediaType.ANIME,
        formats = MovieFormats,
        popularityGreater = BestRatedMinPopularity,
        defaultSortOption = UserListSort.AVERAGE_SCORE,
    )
    DashboardSectionKind.BEST_ANIME_NOW -> MediaCatalogFilter(
        type = MediaType.ANIME,
        formats = AnimeOrMovieFormats,
        status = "RELEASING",
        popularityGreater = BestRatedMinPopularity,
        defaultSortOption = UserListSort.AVERAGE_SCORE,
    )
    DashboardSectionKind.BEST_MOVIE_NOW -> MediaCatalogFilter(
        type = MediaType.ANIME,
        formats = MovieFormats,
        startDateGreater = recentStart,
        popularityGreater = BestRatedMinPopularity,
        defaultSortOption = UserListSort.AVERAGE_SCORE,
    )
    DashboardSectionKind.NEW_RELEASES_MANGA -> MediaCatalogFilter(
        type = MediaType.MANGA,
        startDateGreater = recentStart,
        popularityGreater = NewReleasesMinPopularity,
        defaultSortOption = UserListSort.AVERAGE_SCORE,
    )
    DashboardSectionKind.TRENDING_MANGA -> MediaCatalogFilter(
        type = MediaType.MANGA,
        defaultSortOption = UserListSort.TRENDING,
    )
    DashboardSectionKind.POPULAR_MANGA -> MediaCatalogFilter(
        type = MediaType.MANGA,
        countries = listOf("JP"),
        formats = MangaFormats,
        defaultSortOption = UserListSort.POPULARITY,
    )
    DashboardSectionKind.POPULAR_LIGHT_NOVEL -> MediaCatalogFilter(
        type = MediaType.MANGA,
        formats = LightNovelFormats,
        defaultSortOption = UserListSort.POPULARITY,
    )
    DashboardSectionKind.POPULAR_MANHWA -> MediaCatalogFilter(
        type = MediaType.MANGA,
        countries = listOf("KR"),
        formats = MangaFormats,
        defaultSortOption = UserListSort.POPULARITY,
    )
    DashboardSectionKind.POPULAR_MANHUA -> MediaCatalogFilter(
        type = MediaType.MANGA,
        countries = listOf("CN", "TW"),
        formats = MangaFormats,
        defaultSortOption = UserListSort.POPULARITY,
    )
    DashboardSectionKind.BEST_MANGA -> MediaCatalogFilter(
        type = MediaType.MANGA,
        countries = listOf("JP"),
        formats = MangaFormats,
        popularityGreater = BestRatedMinPopularity,
        defaultSortOption = UserListSort.AVERAGE_SCORE,
    )
    DashboardSectionKind.BEST_LIGHT_NOVEL -> MediaCatalogFilter(
        type = MediaType.MANGA,
        formats = LightNovelFormats,
        popularityGreater = BestRatedMinPopularity,
        defaultSortOption = UserListSort.AVERAGE_SCORE,
    )
    DashboardSectionKind.BEST_MANHWA -> MediaCatalogFilter(
        type = MediaType.MANGA,
        countries = listOf("KR"),
        formats = MangaFormats,
        popularityGreater = BestRatedMinPopularity,
        defaultSortOption = UserListSort.AVERAGE_SCORE,
    )
    DashboardSectionKind.BEST_MANHUA -> MediaCatalogFilter(
        type = MediaType.MANGA,
        countries = listOf("CN", "TW"),
        formats = MangaFormats,
        popularityGreater = BestRatedMinPopularity,
        defaultSortOption = UserListSort.AVERAGE_SCORE,
    )
}
