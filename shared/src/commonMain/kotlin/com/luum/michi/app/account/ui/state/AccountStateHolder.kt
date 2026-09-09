package com.luum.michi.app.account.ui.state

import com.luum.michi.app.account.domain.AccountRepository
import com.luum.michi.app.account.domain.model.AccountFavorites
import com.luum.michi.app.account.domain.model.AccountStats
import com.luum.michi.app.core.domain.network.AniListNetworkPolicy
import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

internal val EmptyStats = AccountStats(
    animeCount = 0,
    mangaCount = 0,
    followingCount = 0,
    followersCount = 0,
)

internal class AccountStateHolder(
    private val repository: AccountRepository,
    private val scope: CoroutineScope,
) {
    private var statsState = EmptyStats
    private var favoritesState = AccountFavorites.EMPTY
    private var loadingState = false
    private var refreshingState = false
    private var errorState: NetworkError? = null
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null
    private var lastUserId: Int? = null

    val stats: AccountStats get() = statsState
    val favorites: AccountFavorites get() = favoritesState
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
                when (val result = repository.loadAccount(userId)) {
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
internal fun createAccountStateHolder(
    repository: AccountRepository,
    scope: CoroutineScope,
    viewerId: Int,
): AccountStateHolder {
    return AccountStateHolder(repository, scope)
}
