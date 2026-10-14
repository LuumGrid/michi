package com.luum.michi.app.discover.domain.model

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

/** One badge a dashboard card may show, in paint order (first = top-start). */
internal enum class DashboardCardStat { SCORE, POPULARITY }

/**
 * Which badges a rail's cards show, in order (max two). "Most popular" shelves
 * lead with the popularity they are sorted by; "best rated" ones and the rest
 * lead with the score. Single source of truth for the rail badges.
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
