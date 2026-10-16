package com.luum.michi.app.profile.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileStats
import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

internal val EmptyStats = ProfileStats(
    animeCount = 0,
    mangaCount = 0,
    followingCount = 0,
    followersCount = 0,
)

internal class ProfileStateHolder(
    private val repository: ProfileRepository,
    private val scope: CoroutineScope,
) {
    private var statsState by mutableStateOf(EmptyStats)
    private var favoritesState by mutableStateOf(ProfileFavorites.EMPTY)
    private var loadingState by mutableStateOf(false)
    private var refreshingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null
    /** Last successful load: the cache shortcut keys on it. */
    private var lastUserId: Int? = null

    /** Last user a load was started for: [refresh] retries this one even if it
     *  failed (a failed switch must not make refresh reload the previous user). */
    private var requestedUserId: Int? = null
    private var loadJob: Job? = null

    val stats: ProfileStats get() = statsState
    val favorites: ProfileFavorites get() = favoritesState
    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState
    val error: NetworkError? get() = errorState

    fun load(userId: Int, forceRefresh: Boolean = false) {
        val mark = lastLoaded
        if (!forceRefresh && lastUserId == userId && mark != null
            && mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL && statsState != EmptyStats
        ) return
        requestedUserId = userId
        // Switching user must never show the previous user's data while the new
        // one loads; a same-user reload keeps it until the response replaces it.
        if (lastUserId != userId) {
            statsState = EmptyStats
            favoritesState = ProfileFavorites.EMPTY
        }
        // Newest load wins: cancel the previous request so a slower (stale)
        // response cannot overwrite newer data. Only the current request writes
        // state and owns the spinners.
        loadJob?.cancel()
        val isRefresh = forceRefresh && statsState != EmptyStats
        loadingState = false
        refreshingState = false
        if (isRefresh) refreshingState = true else loadingState = true
        errorState = null
        // LAZY then start, like PagedList.request: the job is stored before the
        // body runs, so the identity check is valid on an eagerly dispatched scope.
        val request = scope.launch(start = CoroutineStart.LAZY) {
            try {
                when (val result = repository.loadProfile(userId)) {
                    is NetworkResult.Success -> if (loadJob === coroutineContext[Job]) {
                        statsState = result.value.stats
                        favoritesState = result.value.favorites
                        lastLoaded = timeMark.markNow()
                        lastUserId = userId
                    }
                    is NetworkResult.Failure -> if (loadJob === coroutineContext[Job]) {
                        errorState = result.error
                    }
                }
            } finally {
                // Only the current request owns the spinners: a cancelled load
                // must not turn off the one now in flight.
                if (loadJob === coroutineContext[Job]) {
                    loadingState = false
                    refreshingState = false
                }
            }
        }
        loadJob = request
        request.start()
    }

    fun refresh() {
        requestedUserId?.let { load(it, forceRefresh = true) }
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createProfileStateHolder(
    repository: ProfileRepository,
    scope: CoroutineScope,
    viewerId: Int,
): ProfileStateHolder {
    return ProfileStateHolder(repository, scope)
}
