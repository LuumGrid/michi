package com.luum.michi.app.discovery.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.components.PlatformHomeMediaItem
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem

internal data class DiscoveryFeed(
    val releasingToday: List<PlatformHomeReleaseItem>,
    val trendingAnimation: List<PlatformHomeMediaItem>,
    val trendingReading: List<PlatformHomeMediaItem>,
)

internal interface DiscoveryRepository {
    /**
     * Fetches trending anime, trending manga, and the next 24h of airing
     * schedules in a single GraphQL round trip.
     */
    suspend fun loadFeed(): NetworkResult<DiscoveryFeed>
}
