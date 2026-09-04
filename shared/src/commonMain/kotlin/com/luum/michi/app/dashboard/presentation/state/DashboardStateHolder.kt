package com.luum.michi.app.dashboard.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.components.PlatformDiscoverMediaItem
import com.luum.michi.app.dashboard.data.DashboardFeed
import com.luum.michi.app.dashboard.data.DashboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

private val EmptyFeed = DashboardFeed(
    trendingAnime = emptyList(),
    trendingManga = emptyList(),
    thisSeason = emptyList(),
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
    private var refreshingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null

    val trendingAnime: List<PlatformDiscoverMediaItem> get() = feedState.trendingAnime
    val trendingManga: List<PlatformDiscoverMediaItem> get() = feedState.trendingManga
    val thisSeason: List<PlatformDiscoverMediaItem> get() = feedState.thisSeason
    val upcomingNextSeason: List<PlatformDiscoverMediaItem> get() = feedState.upcomingNextSeason
    val allTimePopularAnime: List<PlatformDiscoverMediaItem> get() = feedState.allTimePopularAnime
    val allTimePopularManga: List<PlatformDiscoverMediaItem> get() = feedState.allTimePopularManga
    val topAnime: List<PlatformDiscoverMediaItem> get() = feedState.topAnime
    val topManga: List<PlatformDiscoverMediaItem> get() = feedState.topManga
    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState
    val error: NetworkError? get() = errorState

    /** Load dashboard sections, skipping the network call if data was fetched within the TTL. */
    fun load(forceRefresh: Boolean = false) {
        val mark = lastLoaded
        if (!forceRefresh && mark != null && mark.elapsedNow() < CACHE_TTL && feedState != EmptyFeed) return
        val isRefresh = forceRefresh && feedState != EmptyFeed
        if (isRefresh) {
            refreshingState = true
        } else {
            loadingState = true
        }
        errorState = null
        scope.launch {
            try {
                when (val result = repository.loadFeed()) {
                    is NetworkResult.Success -> {
                        feedState = result.value
                        lastLoaded = timeMark.markNow()
                    }
                    is NetworkResult.Failure -> errorState = result.error
                }
            } finally {
                loadingState = false
                refreshingState = false
            }
        }
    }

    companion object {
        private val CACHE_TTL = 5.minutes
    }
}

@Composable
internal fun rememberDashboardStateHolder(
    repository: DashboardRepository,
): DashboardStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) {
        DashboardStateHolder(repository, scope).also { it.load(forceRefresh = false) }
    }
}
