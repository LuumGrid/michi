package com.luum.michi.app.discover.domain

import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.discover.domain.model.MediaItem

internal data class DashboardFeed(
    val trendingAnime: List<MediaItem>,
    val trendingManga: List<MediaItem>,
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
     */
    suspend fun loadFeed(strings: LanguageStrings): NetworkResult<DashboardFeed>
}
