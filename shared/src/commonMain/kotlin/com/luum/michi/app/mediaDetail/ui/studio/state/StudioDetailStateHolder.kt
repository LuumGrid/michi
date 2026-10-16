package com.luum.michi.app.mediaDetail.ui.studio.state

import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.model.toMediaSort
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.util.DetailCache
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.navigation.domain.DetailHolder
import com.luum.michi.app.core.navigation.domain.GridSortHolder
import com.luum.michi.app.core.model.MediaGridItem
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.ui.state.PagedList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Snapshot stored in the LRU cache for fast back-navigation. */
private data class StudioDetailSnapshot(
    val detail: StudioDetail,
    val media: Page<MediaGridItem>,
    val sortOption: UserListSort,
    val sortOrder: UserListOrder,
)

internal class StudioDetailStateHolder(
    private val repository: StudioDetailRepository,
    private val scope: CoroutineScope,
) : DetailHolder, GridSortHolder {
    // Snapshot-backed: the detail screen reads these directly and must
    // recompose on every load/pagination (plain vars would apply silently).
    var detail: StudioDetail? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set
    var error: NetworkError? by mutableStateOf(null)
        private set
    /** Non-blocking action failure (page load / favourite), shown as a compact row. */
    var actionError: NetworkError? by mutableStateOf(null)
        private set

    private var currentStudioId: Int? = null
    private var currentJob: Job? = null

    private val detailCache = DetailCache<Int, StudioDetailSnapshot>(maxSize = 5)

    /**
     * Works grid. Server order: the grid re-queries from page 1 on a change
     * (see [changeSort]) instead of sorting a loaded page locally, so a studio
     * with hundreds of works shows the real top of the chosen criterion.
     */
    val media = PagedList<MediaGridItem> { a, b -> (a + b).distinctBy { it.mediaId } }

    override var sortOption: UserListSort by mutableStateOf(DEFAULT_SORT_OPTION)
        private set
    override var sortOrder: UserListOrder by mutableStateOf(DEFAULT_SORT_ORDER)
        private set

    override var isFavourite: Boolean by mutableStateOf(false)
        private set
    var isTogglingFavourite: Boolean by mutableStateOf(false)
        private set

    /** The AniList `MediaSort` key the current selection maps to. */
    private fun apiSort(): String = sortOption.toMediaSort(sortOrder, isAnime = true)

    fun load(id: Int) {
        if (currentStudioId == id && (detail != null || isLoading)) return

        val cached = detailCache.get(id)
        if (cached != null) {
            currentStudioId = id
            currentJob?.cancel()
            // A page in flight belongs to what was on screen: never let it
            // append to the snapshot we are restoring (set cancels it).
            detail = cached.detail
            media.set(cached.media)
            sortOption = cached.sortOption
            sortOrder = cached.sortOrder
            isFavourite = cached.detail.isFavourite
            error = null
            actionError = null
            isLoading = false
            return
        }

        currentStudioId = id
        // No cached snapshot: a studio always opens on the default order (the
        // user's last selection is per-studio, restored from the cache above).
        sortOption = DEFAULT_SORT_OPTION
        sortOrder = DEFAULT_SORT_ORDER
        fetchFirstPage(id, clearDetail = true, clearMedia = true)
    }

    override fun refresh() {
        val id = currentStudioId ?: return
        // Keeps the works on screen while reloading (no flicker); only a new
        // studio or a new sort clears them.
        fetchFirstPage(id, clearDetail = false, clearMedia = false)
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
        val id = currentStudioId ?: return
        fetchFirstPage(id, clearDetail = false, clearMedia = true)
    }

    fun loadMore() {
        val id = currentStudioId ?: return
        media.loadMore(
            scope = scope,
            isCurrent = { currentStudioId == id },
            fetch = { page -> repository.loadMediaPage(id, page, apiSort()) },
            onLoaded = {
                actionError = null
                cacheSnapshot(id)
            },
            onError = { actionError = it },
        )
    }

    override fun toggleFavourite() {
        if (isTogglingFavourite) return
        val id = currentStudioId ?: return
        val previous = isFavourite
        val target = !previous
        isFavourite = target
        isTogglingFavourite = true
        scope.launch {
            when (val result = repository.toggleFavourite(id)) {
                is NetworkResult.Success -> {
                    // The cached snapshot of THIS studio gets the new value; the
                    // visible state follows only while this holder still shows it
                    // (the user may have opened another studio meanwhile, and the
                    // response must never overwrite that one).
                    detailCache.update(id) { snapshot ->
                        snapshot.copy(detail = snapshot.detail.copy(isFavourite = target))
                    }
                    if (currentStudioId == id) detail = detail?.copy(isFavourite = target)
                }
                is NetworkResult.Failure -> if (currentStudioId == id) {
                    isFavourite = previous
                    actionError = result.error
                }
            }
            isTogglingFavourite = false
        }
    }

    /** Drops every cached snapshot: a language change re-localizes all of them. */
    override fun invalidateCache() {
        detailCache.clear()
    }

    /** Dismisses the compact action-error row. */
    fun clearActionError() {
        actionError = null
    }

    /**
     * Fetches page 1 with the current sort; the single network path of the
     * holder. [clearMedia] empties the loaded works — a new studio or a new sort
     * invalidates them, while a refresh keeps them on screen until the response
     * replaces them.
     */
    private fun fetchFirstPage(id: Int, clearDetail: Boolean, clearMedia: Boolean) {
        currentJob?.cancel()
        // The new list starts at page 1: an in-flight page (old sort, or another
        // studio) is dropped instead of merging into it or overwriting the page.
        media.cancel()
        if (clearDetail) detail = null
        if (clearMedia) media.clear()
        error = null
        actionError = null
        isLoading = true
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, apiSort())) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detail = value
                    media.set(value.media)
                    isFavourite = value.isFavourite
                    cacheSnapshot(id)
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    private fun cacheSnapshot(id: Int) {
        val value = detail ?: return
        detailCache.put(
            id,
            StudioDetailSnapshot(
                detail = value,
                media = media.snapshot(),
                sortOption = sortOption,
                sortOrder = sortOrder,
            ),
        )
    }
}

private val DEFAULT_SORT_OPTION = UserListSort.RELEASE_DATE
private val DEFAULT_SORT_ORDER = UserListOrder.DESCENDING

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createStudioDetailStateHolder(
    repository: StudioDetailRepository,
    scope: CoroutineScope,
): StudioDetailStateHolder {
    return StudioDetailStateHolder(repository, scope)
}
