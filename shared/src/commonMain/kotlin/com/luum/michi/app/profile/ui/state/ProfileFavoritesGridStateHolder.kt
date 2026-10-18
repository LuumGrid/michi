package com.luum.michi.app.profile.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.core.favourite.domain.FavouriteRepository
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.ui.state.LatestRequest
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope

/**
 * Paginated state for the "see more" favourites grid screen. All five categories
 * share one [ProfileFavorite] list; a category switch is a new search, so the
 * loaded pages are dropped and a page still in flight never lands (generation).
 * A favourite toggled elsewhere ([FavouriteRepository.revision]) makes the loaded
 * page stale: the next open refetches instead of showing the in-memory copy.
 */
internal class ProfileFavoritesGridStateHolder(
    private val repository: ProfileRepository,
    private val favouriteRepository: FavouriteRepository,
    private val scope: CoroutineScope,
) {
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    /** Non-blocking failure of a page load: the loaded page stays on screen. */
    private var actionErrorState by mutableStateOf<NetworkError?>(null)

    private val paginated = PagedList<ProfileFavorite> { a, b -> (a + b).distinctBy { it.id } }

    /** Bumped on every new (userId, category): a page from the previous one never lands. */
    private var generation = 0

    private var loadedUserId: Int? = null
    private var loadedCategory: ProfileFavoritesCategory? = null
    /** Last (userId, category) a load was started for: [refresh]/retry re-run it. */
    private var requestedUserId: Int? = null
    private var requestedCategory: ProfileFavoritesCategory? = null
    /** Favourite revision the loaded page came from; a change drops the copy. */
    private var loadedRevision: Int = 0
    private val loadRequest = LatestRequest()

    val items: List<ProfileFavorite> get() = paginated.items
    val isLoading: Boolean get() = loadingState
    val isLoadingMore: Boolean get() = paginated.isLoadingMore
    val hasNextPage: Boolean get() = paginated.hasNextPage
    /** Current page of the backing PagedList: the auto-paging effect keys on it. */
    val currentPage: Int get() = paginated.currentPage
    val error: NetworkError? get() = errorState
    /** A failed page load (2+): a compact dismissible row, never the blocking
     *  first-page error. */
    val actionError: NetworkError? get() = actionErrorState

    fun clearActionError() {
        actionErrorState = null
    }

    fun load(userId: Int, category: ProfileFavoritesCategory) {
        requestedUserId = userId
        requestedCategory = category
        // Already loaded (same user + category) and no favourite changed since:
        // a re-entry shows the in-memory page without another request.
        val stale = favouriteRepository.revision.value != loadedRevision
        if (!stale && loadedUserId == userId && loadedCategory == category) return
        startFirstPage(userId, category)
    }

    /** Re-runs the current (user, category) page 1: retry after a failure and
     *  pull-to-refresh. No-op before the first [load]. */
    fun refresh() {
        val userId = requestedUserId ?: return
        val category = requestedCategory ?: return
        startFirstPage(userId, category)
    }

    private fun startFirstPage(userId: Int, category: ProfileFavoritesCategory) {
        loadedUserId = userId
        loadedCategory = category
        generation++
        paginated.clear()
        errorState = null
        actionErrorState = null
        loadingState = true
        loadRequest.launch(
            scope = scope,
            onSettled = { loadingState = false },
        ) { isCurrent ->
            when (val result = repository.loadFavoritesPage(userId, category, page = 1)) {
                is NetworkResult.Success -> if (isCurrent()) {
                    paginated.set(result.value)
                    loadedRevision = favouriteRepository.revision.value
                }
                is NetworkResult.Failure -> if (isCurrent()) {
                    errorState = result.error
                    // A failed first page leaves nothing to page from: forget the
                    // markers so a retry via load() is not swallowed by the early-return.
                    loadedUserId = null
                    loadedCategory = null
                }
            }
        }
    }

    fun loadMore() {
        val userId = loadedUserId ?: return
        val category = loadedCategory ?: return
        // A new search is already on its way: page N+1 of it cannot be asked yet.
        if (loadingState) return
        val currentGeneration = generation
        paginated.loadMore(
            scope = scope,
            isCurrent = { generation == currentGeneration },
            fetch = { page -> repository.loadFavoritesPage(userId, category, page) },
            onLoaded = { actionErrorState = null },
            onError = { actionErrorState = it },
        )
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createProfileFavoritesGridStateHolder(
    repository: ProfileRepository,
    favouriteRepository: FavouriteRepository,
    scope: CoroutineScope,
): ProfileFavoritesGridStateHolder {
    return ProfileFavoritesGridStateHolder(repository, favouriteRepository, scope)
}
