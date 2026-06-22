package com.luum.michi.app.account.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.account.data.AccountRepository
import com.luum.michi.app.account.presentation.model.AccountFavoriteMedia
import com.luum.michi.app.account.presentation.model.AccountFavoritePerson
import com.luum.michi.app.account.presentation.model.AccountFavoriteStudio
import com.luum.michi.app.account.presentation.model.AccountFavoritesCategory
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Paginated state for the "see more" favourites grid screen. One instance is
 * scoped to a single (userId, category) pair; the screen recreates it via
 * [rememberAccountFavoritesGridStateHolder] whenever the category changes.
 */
internal class AccountFavoritesGridStateHolder(
    private val repository: AccountRepository,
    private val scope: CoroutineScope,
) {
    private val mediaItemsState = mutableStateListOf<AccountFavoriteMedia>()
    private val personItemsState = mutableStateListOf<AccountFavoritePerson>()
    private val studioItemsState = mutableStateListOf<AccountFavoriteStudio>()
    private var isLoadingState by mutableStateOf(false)
    private var isLoadingMoreState by mutableStateOf(false)
    private var hasNextPageState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    private var currentPage = 0
    private var loadedUserId: Int? = null
    private var loadedCategory: AccountFavoritesCategory? = null

    val mediaItems: List<AccountFavoriteMedia> get() = mediaItemsState
    val personItems: List<AccountFavoritePerson> get() = personItemsState
    val studioItems: List<AccountFavoriteStudio> get() = studioItemsState
    val isLoading: Boolean get() = isLoadingState
    val isLoadingMore: Boolean get() = isLoadingMoreState
    val hasNextPage: Boolean get() = hasNextPageState
    val error: NetworkError? get() = errorState

    fun load(userId: Int, category: AccountFavoritesCategory) {
        if (loadedUserId == userId && loadedCategory == category) return
        loadedUserId = userId
        loadedCategory = category
        currentPage = 1
        mediaItemsState.clear()
        personItemsState.clear()
        studioItemsState.clear()
        hasNextPageState = false
        errorState = null
        isLoadingState = true
        scope.launch {
            try {
                fetchPage(userId, category, page = 1)
            } finally {
                isLoadingState = false
            }
        }
    }

    fun loadMore() {
        val userId = loadedUserId ?: return
        val category = loadedCategory ?: return
        if (isLoadingMoreState || !hasNextPageState) return
        isLoadingMoreState = true
        val nextPage = currentPage + 1
        scope.launch {
            try {
                fetchPage(userId, category, page = nextPage)
            } finally {
                isLoadingMoreState = false
            }
        }
    }

    private suspend fun fetchPage(userId: Int, category: AccountFavoritesCategory, page: Int) {
        when (val result = repository.loadFavoritesPage(userId, category, page)) {
            is NetworkResult.Success -> {
                mediaItemsState.addAll(result.value.mediaItems)
                personItemsState.addAll(result.value.personItems)
                studioItemsState.addAll(result.value.studioItems)
                hasNextPageState = result.value.hasNextPage
                currentPage = page
            }
            is NetworkResult.Failure -> errorState = result.error
        }
    }
}

@Composable
internal fun rememberAccountFavoritesGridStateHolder(
    repository: AccountRepository,
): AccountFavoritesGridStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) {
        AccountFavoritesGridStateHolder(repository, scope)
    }
}
