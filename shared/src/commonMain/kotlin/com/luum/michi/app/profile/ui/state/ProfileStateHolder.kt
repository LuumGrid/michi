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
import com.luum.michi.app.ui.state.LatestRequest
import kotlinx.coroutines.CoroutineScope
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
    private val loadRequest = LatestRequest()

    val stats: ProfileStats get() = statsState
    val favorites: ProfileFavorites get() = favoritesState
    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState
    val error: NetworkError? get() = errorState

    fun load(userId: Int, forceRefresh: Boolean = false) {
        val mark = lastLoaded
        if (!forceRefresh && hasDataFor(userId) && mark != null
            && mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL
        ) return
        requestedUserId = userId
        // Switching user must never show the previous user's data while the new
        // one loads; a same-user reload keeps it until the response replaces it.
        if (lastUserId != userId) {
            statsState = EmptyStats
            favoritesState = ProfileFavorites.EMPTY
        }
        // "Has data" keys on a successful load, not on the stats being non-zero:
        // a real account with everything at 0 is still loaded data.
        val isRefresh = forceRefresh && hasDataFor(userId)
        loadingState = false
        refreshingState = false
        if (isRefresh) refreshingState = true else loadingState = true
        errorState = null
        // Newest load wins: a slower (stale) response cannot overwrite newer
        // data, and only the current request writes state and owns the spinners.
        loadRequest.launch(
            scope = scope,
            onSettled = {
                loadingState = false
                refreshingState = false
            },
        ) { isCurrent ->
            when (val result = repository.loadProfile(userId)) {
                is NetworkResult.Success -> if (isCurrent()) {
                    statsState = result.value.stats
                    favoritesState = result.value.favorites
                    lastLoaded = timeMark.markNow()
                    lastUserId = userId
                }
                is NetworkResult.Failure -> if (isCurrent()) {
                    errorState = result.error
                }
            }
        }
    }

    fun refresh() {
        requestedUserId?.let { load(it, forceRefresh = true) }
    }

    /** True once a load for [userId] succeeded (even with all-zero stats). */
    private fun hasDataFor(userId: Int): Boolean = lastUserId == userId && lastLoaded != null
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createProfileStateHolder(
    repository: ProfileRepository,
    scope: CoroutineScope,
    viewerId: Int,
): ProfileStateHolder {
    return ProfileStateHolder(repository, scope)
}
