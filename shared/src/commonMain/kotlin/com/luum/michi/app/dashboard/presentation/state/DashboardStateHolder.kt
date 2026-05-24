package com.luum.michi.app.dashboard.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.components.PlatformHomeMediaItem
import com.luum.michi.app.core.platform.components.PlatformHomeReleaseItem
import com.luum.michi.app.dashboard.data.DashboardFeed
import com.luum.michi.app.dashboard.data.DashboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val EmptyFeed = DashboardFeed(
    releasingToday = emptyList(),
    trendingAnimation = emptyList(),
    trendingReading = emptyList(),
    popularThisSeason = emptyList(),
    upcomingNextSeason = emptyList(),
    allTimePopularAnime = emptyList(),
    allTimePopularManga = emptyList(),
    topAnime = emptyList(),
    topManga = emptyList(),
)

internal class DashboardStateHolder(
    private val repository: DashboardRepository,
    private val scope: CoroutineScope,
) {
    private var feedState by mutableStateOf(EmptyFeed)
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)

    val releasingToday: List<PlatformHomeReleaseItem> get() = feedState.releasingToday
    val trendingAnimation: List<PlatformHomeMediaItem> get() = feedState.trendingAnimation
    val trendingReading: List<PlatformHomeMediaItem> get() = feedState.trendingReading
    val popularThisSeason: List<PlatformHomeMediaItem> get() = feedState.popularThisSeason
    val upcomingNextSeason: List<PlatformHomeMediaItem> get() = feedState.upcomingNextSeason
    val allTimePopularAnime: List<PlatformHomeMediaItem> get() = feedState.allTimePopularAnime
    val allTimePopularManga: List<PlatformHomeMediaItem> get() = feedState.allTimePopularManga
    val topAnime: List<PlatformHomeMediaItem> get() = feedState.topAnime
    val topManga: List<PlatformHomeMediaItem> get() = feedState.topManga
    val isLoading: Boolean get() = loadingState
    val error: String? get() = errorState

    fun load() {
        loadingState = true
        errorState = null
        scope.launch {
            when (val result = repository.loadFeed()) {
                is NetworkResult.Success -> feedState = result.value
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            loadingState = false
        }
    }
}

@Composable
internal fun rememberDashboardStateHolder(
    repository: DashboardRepository,
): DashboardStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) {
        DashboardStateHolder(repository, scope).also { it.load() }
    }
}
