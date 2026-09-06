package com.luum.michi.app.discover.domain

import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.components.PlatformDiscoverMediaItem

internal data class DashboardFeed(
    val trendingAnime: List<PlatformDiscoverMediaItem>,
    val trendingManga: List<PlatformDiscoverMediaItem>,
    val thisSeason: List<PlatformDiscoverMediaItem>,
    val upcomingNextSeason: List<PlatformDiscoverMediaItem>,
    val allTimePopularAnime: List<PlatformDiscoverMediaItem>,
    val allTimePopularManga: List<PlatformDiscoverMediaItem>,
    val topAnime: List<PlatformDiscoverMediaItem>,
    val topManga: List<PlatformDiscoverMediaItem>,
)

internal interface DashboardRepository {
    /**
     * Fetches trending, seasonal, and popular media in a single GraphQL round trip.
     */
    suspend fun loadFeed(strings: LanguageStrings): NetworkResult<DashboardFeed>
}
