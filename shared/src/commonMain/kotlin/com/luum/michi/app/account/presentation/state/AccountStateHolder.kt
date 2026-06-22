package com.luum.michi.app.account.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.account.data.AccountRepository
import com.luum.michi.app.account.presentation.model.AccountFavorites
import com.luum.michi.app.account.presentation.model.AccountStats
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

private val EmptyStats = AccountStats(
    animeCount = 0,
    mangaCount = 0,
    followingCount = 0,
    followersCount = 0,
)

private val EmptyFavorites = AccountFavorites(
    anime = emptyList(),
    manga = emptyList(),
    characters = emptyList(),
    staff = emptyList(),
    studios = emptyList(),
)

internal class AccountStateHolder(
    private val repository: AccountRepository,
    private val scope: CoroutineScope,
) {
    private var statsState by mutableStateOf(EmptyStats)
    private var favoritesState by mutableStateOf(EmptyFavorites)
    private var loadingState by mutableStateOf(false)
    private var refreshingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
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
            && mark.elapsedNow() < CACHE_TTL && statsState != EmptyStats
        ) return
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

    companion object {
        private val CACHE_TTL = 5.minutes
    }
}

@Composable
internal fun rememberAccountStateHolder(
    repository: AccountRepository,
    viewerId: Int,
): AccountStateHolder {
    val scope = rememberCoroutineScope()
    return remember(viewerId) {
        AccountStateHolder(repository, scope)
    }
}
