package com.luum.michi.app.dashboard.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.components.PlatformHomeMediaItem

internal data class DashboardFeed(
    val trendingAnimation: List<PlatformHomeMediaItem>,
    val trendingReading: List<PlatformHomeMediaItem>,
    val thisSeason: List<PlatformHomeMediaItem>,
    val upcomingNextSeason: List<PlatformHomeMediaItem>,
    val allTimePopularAnime: List<PlatformHomeMediaItem>,
    val allTimePopularManga: List<PlatformHomeMediaItem>,
    val topAnime: List<PlatformHomeMediaItem>,
    val topManga: List<PlatformHomeMediaItem>,
)

internal interface DashboardRepository {
    /**
     * Fetches trending, seasonal, and popular media in a single GraphQL round trip.
     */
    suspend fun loadFeed(): NetworkResult<DashboardFeed>
}
