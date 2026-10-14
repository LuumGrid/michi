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
