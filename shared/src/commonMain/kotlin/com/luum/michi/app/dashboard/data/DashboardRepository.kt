package com.luum.michi.app.dashboard.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.components.PlatformHomeMediaItem
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem

internal data class DashboardFeed(
    val releasingToday: List<PlatformHomeReleaseItem>,
    val trendingAnimation: List<PlatformHomeMediaItem>,
    val trendingReading: List<PlatformHomeMediaItem>,
    val popularThisSeason: List<PlatformHomeMediaItem>,
    val upcomingNextSeason: List<PlatformHomeMediaItem>,
    val allTimePopularAnime: List<PlatformHomeMediaItem>,
    val allTimePopularManga: List<PlatformHomeMediaItem>,
    val topAnime: List<PlatformHomeMediaItem>,
    val topManga: List<PlatformHomeMediaItem>,
)

internal interface DashboardRepository {
    /**
     * Fetches trending anime, trending manga, and the next 24h of airing
     * schedules in a single GraphQL round trip.
     */
    suspend fun loadFeed(): NetworkResult<DashboardFeed>
}
