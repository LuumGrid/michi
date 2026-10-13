package com.luum.michi.app.discover.ui.dashboard.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.discover.domain.model.MediaItem
import com.luum.michi.app.discover.domain.DashboardFeed
import com.luum.michi.app.discover.domain.DashboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    private val strings: LanguageStrings,
) {
    // Compose-backed: the dashboard reads these directly and must recompose
    // on every load/refresh.
    private var feedState: DashboardFeed by mutableStateOf(EmptyFeed)
    private var loadingState: Boolean by mutableStateOf(false)
    private var refreshingState: Boolean by mutableStateOf(false)
    private var errorState: NetworkError? by mutableStateOf(null)
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null

    // 18+ gate, owned here so a change can force a reload. Hidden by default
    // (matches the settings default); `false` lets the server return both.
    private var hideAdultState: Boolean = true

    val trendingAnime: List<MediaItem> get() = feedState.trendingAnime
    val trendingManga: List<MediaItem> get() = feedState.trendingManga
    val thisSeason: List<MediaItem> get() = feedState.thisSeason
    val upcomingNextSeason: List<MediaItem> get() = feedState.upcomingNextSeason
    val allTimePopularAnime: List<MediaItem> get() = feedState.allTimePopularAnime
    val allTimePopularManga: List<MediaItem> get() = feedState.allTimePopularManga
    val topAnime: List<MediaItem> get() = feedState.topAnime
    val topManga: List<MediaItem> get() = feedState.topManga
    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState
    val error: NetworkError? get() = errorState

    /** Mirror the 18+ setting; a change forces a reload so the feed matches. */
    fun setHideAdult(hideAdult: Boolean) {
        if (hideAdultState == hideAdult) return
        hideAdultState = hideAdult
        load(forceRefresh = true)
    }

    /** Load dashboard sections, skipping the network call if data was fetched within the TTL. */
    fun load(forceRefresh: Boolean = false) {
        val mark = lastLoaded
        if (!forceRefresh && mark != null && mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL && feedState != EmptyFeed) return
        // Drop overlapping loads so a slower (stale) response cannot overwrite a newer one.
        if (loadingState || refreshingState) return
        val isRefresh = forceRefresh && feedState != EmptyFeed
        if (isRefresh) {
            refreshingState = true
        } else {
            loadingState = true
        }
        errorState = null
        scope.launch {
            try {
                when (val result = repository.loadFeed(hideAdultState, strings)) {
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
}

/** Pure-logic factory. Loading is explicit (the screen triggers it), so
 *  creating a holder never fires a request. */
internal fun createDashboardStateHolder(
    repository: DashboardRepository,
    scope: CoroutineScope,
    strings: LanguageStrings,
): DashboardStateHolder {
    return DashboardStateHolder(repository, scope, strings)
}
