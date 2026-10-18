package com.luum.michi.app.discover.ui.catalog.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.medialist.domain.toMediaSort
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.navigation.domain.DetailHolder
import com.luum.michi.app.core.navigation.domain.GridSortHolder
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.util.DetailCache
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.model.MediaCatalogFilter
import com.luum.michi.app.ui.state.LatestRequest
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope

/** Cache key: one snapshot per filter + 18+ gate, so a cached snapshot can never
 *  be restored under a gate it was not fetched with. */
private data class CatalogGridKey(val filter: MediaCatalogFilter, val hideAdult: Boolean)

/** Snapshot stored in the LRU cache for fast back-navigation. */
private data class CatalogGridSnapshot(
    val media: Page<MediaGridItem>,
    val sortOption: UserListSort,
    val sortOrder: UserListOrder,
)

/**
 * One server-ordered catalog grid, shared by a genre page and a Dashboard "View
 * all" catalog: both are just a [MediaCatalogFilter] over the same [ExploreRepository]
 * grid query (a genre is `type + genre_in`). [pageLoaded] is the "there is
 * something to show" flag the screen hands to `LoadStateContainer`.
 *
 * The holder is a single instance for both destinations (the detail stack swaps
 * it by loading a new filter), so the LRU cache holds genres and catalogs alike,
 * keyed by filter + 18+ gate. Server order: a sort change re-queries page 1
 * instead of re-sorting the loaded page; [refresh] keeps the works on screen.
 */
internal class CatalogGridStateHolder(
    private val repository: ExploreRepository,
    private val scope: CoroutineScope,
    hideAdult: Boolean,
) : DetailHolder, GridSortHolder {
    // Snapshot-backed: the screen reads these directly and must recompose on
    // every load/pagination (plain vars would apply silently).
    var filter: MediaCatalogFilter by mutableStateOf(MediaCatalogFilter(type = MediaType.ANIME))
        private set
    /** True once page 1 answered (or a cache snapshot restored). */
    var pageLoaded: Boolean by mutableStateOf(false)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set
    var error: NetworkError? by mutableStateOf(null)
        private set
    /** Non-blocking action failure (page load), shown as a compact row. */
    var actionError: NetworkError? by mutableStateOf(null)
        private set

    /** Works grid; the server orders it (re-queried from page 1 on a change). */
    val media = PagedList<MediaGridItem> { a, b -> (a + b).distinctBy { it.mediaId } }

    override var sortOption: UserListSort by mutableStateOf(UserListSort.POPULARITY)
        private set
    override var sortOrder: UserListOrder by mutableStateOf(UserListOrder.DESCENDING)
        private set

    private var hideAdultState: Boolean = hideAdult
    private val firstPageRequest = LatestRequest()

    private val detailCache = DetailCache<CatalogGridKey, CatalogGridSnapshot>(maxSize = 5)

    /** The AniList `MediaSort` key the current selection maps to. */
    private fun apiSort(): String = sortOption.toMediaSort(sortOrder, isAnime = filter.type == MediaType.ANIME)

    /** Binds the grid to [filter]. No-op when it is already the one on screen; a
     *  cached filter restores its snapshot (and its sort) with no request. */
    fun load(filter: MediaCatalogFilter) {
        if (this.filter == filter && (pageLoaded || isLoading)) return

        val cached = detailCache.get(CatalogGridKey(filter, hideAdultState))
        this.filter = filter
        if (cached != null) {
            firstPageRequest.cancel()
            // A page in flight belongs to what was on screen: never let it
            // append to the snapshot we are restoring (set cancels it).
            restore(cached)
            return
        }
        // No cached snapshot: a catalog opens on its rail's default order.
        sortOption = filter.defaultSortOption
        sortOrder = filter.defaultSortOrder
        fetchFirstPage(clearMedia = true, showSpinner = true)
    }

    /**
     * Genre only: switch the catalog (Anime ↔ Manga), keeping the rest of the
     * filter. A catalog already visited restores its own snapshot (and sort).
     */
    fun changeType(type: MediaType) {
        if (type == filter.type) return
        load(filter.copy(type = type))
    }

    override fun refresh() {
        // Keeps the works on screen while reloading (no flicker).
        fetchFirstPage(clearMedia = false, showSpinner = false)
    }

    /**
     * Sort change: the server orders, so the grid re-queries from page 1 with
     * the new `MediaSort` (a local re-sort would only reorder the loaded page).
     * No-op when nothing changes, so re-picking the same row never fires.
     */
    override fun changeSort(option: UserListSort, order: UserListOrder) {
        if (option == sortOption && order == sortOrder) return
        sortOption = option
        sortOrder = order
        fetchFirstPage(clearMedia = true, showSpinner = false)
    }

    /**
     * 18+ gate: flipping it re-queries the current page 1 (the grid keeps its
     * works until the answer lands). Never sends `isAdult: true` — see the repo.
     */
    fun setHideAdult(value: Boolean) {
        if (value == hideAdultState) return
        hideAdultState = value
        if (pageLoaded || isLoading) fetchFirstPage(clearMedia = false, showSpinner = false)
    }

    fun loadMore() {
        val bound = filter
        media.loadMore(
            scope = scope,
            isCurrent = { filter == bound },
            fetch = { page -> loadPage(page) },
            onLoaded = {
                actionError = null
                cacheSnapshot()
            },
            onError = { actionError = it },
        )
    }

    /**
     * Fetches page 1 for the current filter + sort; the single network path of
     * the holder. [clearMedia] empties the loaded works — a new filter or sort
     * invalidates them, while a refresh keeps them on screen. [showSpinner]
     * drops back to the first-load state.
     */
    private fun fetchFirstPage(clearMedia: Boolean, showSpinner: Boolean) {
        // The new list starts at page 1: an in-flight page (old filter/sort) is
        // dropped instead of merging into it.
        media.cancel()
        if (clearMedia) media.clear()
        if (showSpinner) pageLoaded = false
        error = null
        actionError = null
        isLoading = true
        firstPageRequest.launch(
            scope = scope,
            onSettled = { isLoading = false },
        ) { isCurrent ->
            when (val result = loadPage(1)) {
                is NetworkResult.Success -> if (isCurrent()) {
                    media.set(result.value)
                    pageLoaded = true
                    cacheSnapshot()
                }
                is NetworkResult.Failure -> if (isCurrent()) {
                    error = result.error
                }
            }
        }
    }

    /** One grid page for the current filter + sort + gate. */
    private suspend fun loadPage(page: Int) = repository.searchMediaGrid(
        filter = filter,
        page = page,
        sort = apiSort(),
        hideAdult = hideAdultState,
    )

    /** Puts a cached snapshot back on screen (works, cursor and sort). */
    private fun restore(snapshot: CatalogGridSnapshot) {
        media.set(snapshot.media)
        sortOption = snapshot.sortOption
        sortOrder = snapshot.sortOrder
        pageLoaded = true
        error = null
        actionError = null
        isLoading = false
    }

    /** Drops every cached snapshot: a language change re-localizes all of them. */
    override fun invalidateCache() {
        detailCache.clear()
    }

    /** A catalog is not a favouritable entity: nothing to report or toggle. */
    override val isFavourite: Boolean get() = false

    override fun toggleFavourite() = Unit

    /** Dismisses the compact action-error row. */
    fun clearActionError() {
        actionError = null
    }

    private fun cacheSnapshot() {
        detailCache.put(
            CatalogGridKey(filter, hideAdultState),
            CatalogGridSnapshot(
                media = media.snapshot(),
                sortOption = sortOption,
                sortOrder = sortOrder,
            ),
        )
    }
}
