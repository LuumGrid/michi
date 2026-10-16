package com.luum.michi.app.discover.ui.genre.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.medialist.domain.toMediaSort
import com.luum.michi.app.core.navigation.domain.DetailHolder
import com.luum.michi.app.core.navigation.domain.GridSortHolder
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.util.DetailCache
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Cache key: one snapshot per genre + catalog + 18+ gate, so a cached snapshot
 *  can never be restored under a gate it was not fetched with. */
private data class GenreKey(val name: String, val type: MediaType, val hideAdult: Boolean)

/** Snapshot stored in the LRU cache for fast back-navigation. */
private data class GenreSnapshot(
    val media: Page<MediaGridItem>,
    val sortOption: UserListSort,
    val sortOrder: UserListOrder,
)

/**
 * State of one genre page. A genre has no entity, so the holder keeps the genre
 * and catalog it was loaded with instead of a detail model; [pageLoaded] is the
 * "there is something to show" flag the screen hands to `LoadStateContainer`.
 *
 * A genre is just the catalog filtered by name, so it reads from the shared
 * [ExploreRepository] grid query. Server order: the grid re-queries from page 1
 * on a sort (or catalog) change instead of sorting a loaded page locally, so a
 * genre with thousands of works shows the real top of the chosen criterion.
 * [refresh] keeps the loaded works on screen while reloading; a catalog switch
 * clears them.
 */
internal class GenreDetailStateHolder(
    private val repository: ExploreRepository,
    private val scope: CoroutineScope,
    hideAdult: Boolean,
) : DetailHolder, GridSortHolder {
    // Snapshot-backed: the screen reads these directly and must recompose on
    // every load/pagination (plain vars would apply silently).
    var name: String by mutableStateOf("")
        private set
    var type: MediaType by mutableStateOf(MediaType.ANIME)
        private set
    /** True once page 1 answered (or a cache snapshot restored), so the screen
     *  shows the grid instead of the first spinner. */
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

    override var sortOption: UserListSort by mutableStateOf(DEFAULT_SORT_OPTION)
        private set
    override var sortOrder: UserListOrder by mutableStateOf(DEFAULT_SORT_ORDER)
        private set

    private var hideAdultState: Boolean = hideAdult
    private var currentJob: Job? = null

    private val detailCache = DetailCache<GenreKey, GenreSnapshot>(maxSize = 5)

    /** The AniList `MediaSort` key the current selection maps to. */
    private fun apiSort(): String = sortOption.toMediaSort(sortOrder, isAnime = type == MediaType.ANIME)

    fun load(name: String, type: MediaType) {
        if (this.name == name && this.type == type && (pageLoaded || isLoading)) return

        val cached = detailCache.get(GenreKey(name, type, hideAdultState))
        this.name = name
        this.type = type
        if (cached != null) {
            currentJob?.cancel()
            // A page in flight belongs to what was on screen: never let it
            // append to the snapshot we are restoring (set cancels it).
            restore(cached)
            return
        }
        // No cached snapshot: a genre always opens on the default order (the
        // user's last selection is per-genre+catalog, restored from the cache).
        sortOption = DEFAULT_SORT_OPTION
        sortOrder = DEFAULT_SORT_ORDER
        fetchFirstPage(clearMedia = true, showSpinner = true)
    }

    override fun refresh() {
        if (name.isEmpty()) return
        // Keeps the works on screen while reloading (no flicker).
        fetchFirstPage(clearMedia = false, showSpinner = false)
    }

    /**
     * Catalog switch (Anime ↔ Manga): the other catalog's works have nothing to
     * do with the loaded ones, so the grid clears and re-queries from page 1. A
     * catalog already visited restores its own snapshot (and its own sort).
     */
    fun changeType(type: MediaType) {
        if (type == this.type) return
        val cached = detailCache.get(GenreKey(name, type, hideAdultState))
        this.type = type
        if (cached != null) {
            currentJob?.cancel()
            restore(cached)
            return
        }
        fetchFirstPage(clearMedia = true, showSpinner = false)
    }

    /**
     * Sort change: the server orders, so the grid re-queries from page 1 with the
     * new `MediaSort` (a local re-sort would only reorder the loaded page). No-op
     * when nothing changes, so re-picking the same row never fires a request.
     */
    override fun changeSort(option: UserListSort, order: UserListOrder) {
        if (option == sortOption && order == sortOrder) return
        sortOption = option
        sortOrder = order
        fetchFirstPage(clearMedia = true, showSpinner = false)
    }

    /**
     * 18+ gate: flipping it re-queries the current page 1 (the grid keeps its
     * works until the answer lands), so the setting applies without leaving the
     * page. Never sends `isAdult: true` — see the repository.
     */
    fun setHideAdult(value: Boolean) {
        if (value == hideAdultState) return
        hideAdultState = value
        if (name.isNotEmpty()) fetchFirstPage(clearMedia = false, showSpinner = false)
    }

    fun loadMore() {
        val genre = name
        val catalog = type
        media.loadMore(
            scope = scope,
            isCurrent = { name == genre && type == catalog },
            fetch = { page -> loadPage(page) },
            onLoaded = {
                actionError = null
                cacheSnapshot()
            },
            onError = { actionError = it },
        )
    }

    /**
     * Fetches page 1 with the current catalog and sort; the single network path
     * of the holder. [clearMedia] empties the loaded works — a new genre or a new
     * sort invalidates them, while a refresh keeps them on screen until the
     * response replaces them. [showSpinner] drops back to the first-load state.
     */
    private fun fetchFirstPage(clearMedia: Boolean, showSpinner: Boolean) {
        currentJob?.cancel()
        // The new list starts at page 1: an in-flight page (old sort, or another
        // catalog) is dropped instead of merging into it.
        media.cancel()
        if (clearMedia) media.clear()
        if (showSpinner) pageLoaded = false
        error = null
        actionError = null
        isLoading = true
        currentJob = scope.launch {
            when (val result = loadPage(1)) {
                is NetworkResult.Success -> {
                    media.set(result.value)
                    pageLoaded = true
                    cacheSnapshot()
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    /** One grid page for the current genre + catalog + sort + gate. */
    private suspend fun loadPage(page: Int) = repository.searchMediaGrid(
        type = type,
        genres = listOf(name),
        page = page,
        sort = apiSort(),
        hideAdult = hideAdultState,
    )

    /** Puts a cached snapshot back on screen (works, cursor and sort). */
    private fun restore(snapshot: GenreSnapshot) {
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

    /** A genre is not a favouritable entity: nothing to report or toggle. */
    override val isFavourite: Boolean get() = false

    override fun toggleFavourite() = Unit

    /** Dismisses the compact action-error row. */
    fun clearActionError() {
        actionError = null
    }

    private fun cacheSnapshot() {
        detailCache.put(
            GenreKey(name, type, hideAdultState),
            GenreSnapshot(
                media = media.snapshot(),
                sortOption = sortOption,
                sortOrder = sortOrder,
            ),
        )
    }
}

private val DEFAULT_SORT_OPTION = UserListSort.POPULARITY
private val DEFAULT_SORT_ORDER = UserListOrder.DESCENDING

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createGenreDetailStateHolder(
    repository: ExploreRepository,
    scope: CoroutineScope,
    hideAdult: Boolean,
): GenreDetailStateHolder = GenreDetailStateHolder(repository, scope, hideAdult)
