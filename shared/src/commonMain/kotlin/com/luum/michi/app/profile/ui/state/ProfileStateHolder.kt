package com.luum.michi.app.profile.ui.state

import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileStats
import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.CoroutineScope
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
    private var statsState = EmptyStats
    private var favoritesState = ProfileFavorites.EMPTY
    private var loadingState = false
    private var refreshingState = false
    private var errorState: NetworkError? = null
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null
    private var lastUserId: Int? = null

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
        // Drop overlapping loads so a slower (stale) response cannot overwrite a newer one.
        if (loadingState || refreshingState) return
        val isRefresh = forceRefresh && statsState != EmptyStats
        if (isRefresh) refreshingState = true else loadingState = true
        errorState = null
        scope.launch {
            try {
                when (val result = repository.loadProfile(userId)) {
                    is NetworkResult.Success -> {
                        statsState = result.value.stats
                        favoritesState = result.value.favorites
                        lastLoaded = timeMark.markNow()
                        lastUserId = userId
                    }
                    is NetworkResult.Failure -> errorState = result.error
                }
            } finally {
                loadingState = false
                refreshingState = false
            }
        }
    }

    fun refresh() {
        lastUserId?.let { load(it, forceRefresh = true) }
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
