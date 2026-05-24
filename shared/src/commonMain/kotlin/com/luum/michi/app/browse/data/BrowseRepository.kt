package com.luum.michi.app.browse.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.components.PlatformHomeMediaItem

internal data class BrowseFeed(
    val popularThisSeason: List<PlatformHomeMediaItem>,
    val upcomingNextSeason: List<PlatformHomeMediaItem>,
    val allTimePopularAnime: List<PlatformHomeMediaItem>,
    val allTimePopularManga: List<PlatformHomeMediaItem>,
    val topAnime: List<PlatformHomeMediaItem>,
    val topManga: List<PlatformHomeMediaItem>,
)

internal interface BrowseRepository {
    suspend fun loadFeed(): NetworkResult<BrowseFeed>
}
