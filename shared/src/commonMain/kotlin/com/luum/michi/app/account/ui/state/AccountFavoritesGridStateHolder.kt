package com.luum.michi.app.account.ui.state

import com.luum.michi.app.account.domain.AccountRepository
import com.luum.michi.app.account.domain.model.AccountFavoriteMedia
import com.luum.michi.app.account.domain.model.AccountFavoritePerson
import com.luum.michi.app.account.domain.model.AccountFavoriteStudio
import com.luum.michi.app.account.domain.model.AccountFavoritesCategory
import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult
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
    private val mediaItemsState = mutableListOf<AccountFavoriteMedia>()
    private val personItemsState = mutableListOf<AccountFavoritePerson>()
    private val studioItemsState = mutableListOf<AccountFavoriteStudio>()
    private var isLoadingState = false
    private var isLoadingMoreState = false
    private var hasNextPageState = false
    private var errorState: NetworkError? = null
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
                // Las páginas pueden solaparse (el orden por favourites cambia entre
                // requests) o llegar un fetch viejo tras cambiar de categoría: solo
                // se agrega lo que no esté ya, o el grid revienta por keys duplicadas.
                mediaItemsState.addAll(result.value.mediaItems.filter { fresh ->
                    mediaItemsState.none { it.id == fresh.id }
                })
                personItemsState.addAll(result.value.personItems.filter { fresh ->
                    personItemsState.none { it.id == fresh.id }
                })
                studioItemsState.addAll(result.value.studioItems.filter { fresh ->
                    studioItemsState.none { it.id == fresh.id }
                })
                hasNextPageState = result.value.hasNextPage
                currentPage = page
            }
            is NetworkResult.Failure -> errorState = result.error
        }
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createAccountFavoritesGridStateHolder(
    repository: AccountRepository,
    scope: CoroutineScope,
): AccountFavoritesGridStateHolder {
    return AccountFavoritesGridStateHolder(repository, scope)
}
