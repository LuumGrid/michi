package com.luum.michi.app.account.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.account.data.AccountFavoritesRepository
import com.luum.michi.app.account.data.AccountStatsRepository
import com.luum.michi.app.account.presentation.model.AccountFavorites
import com.luum.michi.app.account.presentation.model.AccountStats
import com.luum.michi.app.core.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
    private val statsRepository: AccountStatsRepository,
    private val favoritesRepository: AccountFavoritesRepository,
    private val scope: CoroutineScope,
) {
    private var statsState by mutableStateOf(EmptyStats)
    private var favoritesState by mutableStateOf(EmptyFavorites)
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)

    val stats: AccountStats get() = statsState
    val favorites: AccountFavorites get() = favoritesState
    val isLoading: Boolean get() = loadingState
    val error: String? get() = errorState

    fun load(userId: Int) {
        loadingState = true
        errorState = null

        scope.launch {
            when (val result = statsRepository.loadStats(userId)) {
                is NetworkResult.Success -> statsState = result.value
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            loadingState = false
        }

        scope.launch {
            when (val result = favoritesRepository.loadFavorites(userId)) {
                is NetworkResult.Success -> favoritesState = result.value
                is NetworkResult.Failure -> {
                    if (errorState == null) errorState = result.error.toString()
                }
            }
        }
    }
}

@Composable
internal fun rememberAccountStateHolder(
    statsRepository: AccountStatsRepository,
    favoritesRepository: AccountFavoritesRepository,
    viewerId: Int,
): AccountStateHolder {
    val scope = rememberCoroutineScope()
    return remember(viewerId) {
        AccountStateHolder(statsRepository, favoritesRepository, scope)
    }
}
