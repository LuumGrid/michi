package com.luum.michi.app.discover.domain

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.discover.domain.model.MediaItem

internal data class DashboardFeed(
    val trendingAnime: List<MediaItem>,
    val trendingManga: List<MediaItem>,
    val newReleasesManga: List<MediaItem>,
    val thisSeason: List<MediaItem>,
    val upcomingNextSeason: List<MediaItem>,
    val allTimePopularAnime: List<MediaItem>,
    val allTimePopularManga: List<MediaItem>,
    val topAnime: List<MediaItem>,
    val topManga: List<MediaItem>,
)

internal interface DashboardRepository {
    /**
     * Fetches trending, seasonal, and popular media in a single GraphQL round trip.
     *
     * [hideAdult] drives the client-side 18+ gate: when true the query pins
     * `isAdult: false`; when false the variable is omitted (null = both).
     * Never sends `isAdult: true` (that would filter to adult ONLY).
     */
    suspend fun loadFeed(hideAdult: Boolean, strings: LanguageStrings): NetworkResult<DashboardFeed>
}
