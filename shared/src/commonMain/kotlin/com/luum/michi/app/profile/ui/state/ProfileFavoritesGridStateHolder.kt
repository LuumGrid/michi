package com.luum.michi.app.profile.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.favourite.domain.FavouriteRepository
import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.domain.model.ProfileFavoritesPageSize
import com.luum.michi.app.profile.ui.model.toFavouriteTarget
import com.luum.michi.app.ui.state.LatestRequest
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Paginated state for the "see more" favourites grid, and the home of the
 * favourites edit mode: removals are optimistic (a local hide), one toggle
 * request, rollback on failure and a cache the profile mirrors. All five
 * categories share one [ProfileFavorite] list; a category switch is a new
 * search, so the loaded pages are dropped and a page still in flight never
 * lands (generation).
 *
 * A favourite toggled elsewhere ([FavouriteRepository.revision]) makes the
 * loaded page stale: the next open refetches instead of showing the in-memory
 * copy.
 */
internal class ProfileFavoritesGridStateHolder(
    private val repository: ProfileRepository,
    private val favouriteRepository: FavouriteRepository,
    private val scope: CoroutineScope,
    // Root's favourite router: a removal flips the detail hearts (optimistic, and
    // reverted on failure) with no feature import.
    private val onFavouriteChanged: (FavouriteTarget, Boolean) -> Unit = { _, _ -> },
    // Tells the profile to drop the item from memory + disk, with no request.
    private val onFavoriteRemoved: (ProfileFavoritesCategory, Int) -> Unit = { _, _ -> },
) {
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    /** Non-blocking failure of a page load: the loaded page stays on screen. */
    private var actionErrorState by mutableStateOf<NetworkError?>(null)
    /** Removals in flight: their item is hidden and disabled, never double-fired. */
    private var removingIds by mutableStateOf(emptySet<Int>())
    /** Removals that succeeded: kept hidden (the server list no longer has them). */
    private var removedIds by mutableStateOf(emptySet<Int>())
    /**
     * A counter bumped on every finished load. The screen feeds it to the
     * auto-paging footer instead of the page number: re-reading the same page
     * (a post-removal rewind) must still re-arm the footer, which a page number
     * that does not change never would.
     */
    var loadKey by mutableStateOf(0)
        private set

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
    /** Successful removals since the last finished load: how far back to rewind. */
    private var removalsSinceLoad = 0
    private val loadRequest = LatestRequest()

    /** The page as painted: [paginated] minus the in-flight and removed ids. */
    val items: List<ProfileFavorite>
        get() = paginated.items.filterNot { it.id in removingIds || it.id in removedIds }

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

    /** True while [id]'s removal is in flight (its button is off). */
    fun isRemoving(id: Int): Boolean = id in removingIds

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

    /**
     * Removes one favourite: optimistic (hidden now), one toggle request,
     * rollback on failure. Success rewrites the profile's memory + cache through
     * [onFavoriteRemoved].
     */
    fun removeFavorite(id: Int) {
        val userId = requestedUserId ?: return
        val category = requestedCategory ?: return
        if (id in removingIds) return
        if (paginated.items.none { it.id == id }) return
        removingIds = removingIds + id
        actionErrorState = null
        val target = category.toFavouriteTarget(id)
        onFavouriteChanged(target, false)
        scope.launch {
            when (val result = favouriteRepository.toggleFavourite(target)) {
                is NetworkResult.Success -> {
                    removingIds = removingIds - id
                    removedIds = removedIds + id
                    removalsSinceLoad++
                    loadedRevision = favouriteRepository.revision.value
                    onFavoriteRemoved(category, id)
                }
                is NetworkResult.Failure -> {
                    removingIds = removingIds - id
                    actionErrorState = result.error
                    onFavouriteChanged(target, true)
                }
            }
        }
    }

    private fun startFirstPage(userId: Int, category: ProfileFavoritesCategory) {
        loadedUserId = userId
        loadedCategory = category
        generation++
        paginated.clear()
        errorState = null
        actionErrorState = null
        loadingState = true
        // Removals already applied when this load starts: the fresh page folds
        // them in, so only the ones landing during the load stay filtered.
        val removedAtLoadStart = removedIds
        loadRequest.launch(
            scope = scope,
            onSettled = { loadingState = false },
        ) { isCurrent ->
            when (val result = repository.loadFavoritesPage(userId, category, page = 1)) {
                is NetworkResult.Success -> if (isCurrent()) {
                    paginated.set(result.value)
                    removedIds = removedIds - removedAtLoadStart
                    removalsSinceLoad = 0
                    loadedRevision = favouriteRepository.revision.value
                    loadKey++
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
        // After removals, AniList has shifted everything up by k: re-read from
        // the page whose window now covers the gap, then advance normally.
        val rewindPage = if (removalsSinceLoad > 0) rewindPage() else null
        paginated.loadMore(
            scope = scope,
            isCurrent = { generation == currentGeneration },
            fetch = { page -> repository.loadFavoritesPage(userId, category, page) },
            page = rewindPage,
            onLoaded = {
                actionErrorState = null
                removalsSinceLoad = 0
                loadKey++
            },
            onError = { actionErrorState = it },
        )
    }

    /**
     * The page a post-removal load must re-read: `k` removals shift the list up
     * by `k`, so with one page (25) covering one window, the rewind starts
     * `(k - 1) / 25` pages earlier (never below 1). k ≤ 25 keeps the same page.
     */
    private fun rewindPage(): Int {
        val pagesBack = (removalsSinceLoad - 1) / ProfileFavoritesPageSize
        return (paginated.currentPage - pagesBack).coerceAtLeast(1)
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createProfileFavoritesGridStateHolder(
    repository: ProfileRepository,
    favouriteRepository: FavouriteRepository,
    scope: CoroutineScope,
    onFavouriteChanged: (FavouriteTarget, Boolean) -> Unit = { _, _ -> },
    onFavoriteRemoved: (ProfileFavoritesCategory, Int) -> Unit = { _, _ -> },
): ProfileFavoritesGridStateHolder {
    return ProfileFavoritesGridStateHolder(
        repository = repository,
        favouriteRepository = favouriteRepository,
        scope = scope,
        onFavouriteChanged = onFavouriteChanged,
        onFavoriteRemoved = onFavoriteRemoved,
    )
}
