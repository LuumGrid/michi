package com.luum.michi.app.mediaDetail.ui.studio.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.medialist.domain.toMediaSort
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.navigation.domain.GridSortHolder
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.ui.state.DetailStateHolderBase
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope

/** Snapshot stored in the LRU cache for fast back-navigation. */
internal data class StudioDetailSnapshot(
    val detail: StudioDetail,
    val media: Page<MediaGridItem>,
    val sortOption: UserListSort,
    val sortOrder: UserListOrder,
)

internal class StudioDetailStateHolder(
    private val repository: StudioDetailRepository,
    scope: CoroutineScope,
) : DetailStateHolderBase<StudioDetail, StudioDetailSnapshot>(scope), GridSortHolder {

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

    override fun isFavouriteOf(detail: StudioDetail): Boolean = detail.isFavourite

    /** The AniList `MediaSort` key the current selection maps to. */
    private fun apiSort(): String = sortOption.toMediaSort(sortOrder, isAnime = true)

    /**
     * Sort change: the server orders, so the grid re-queries from page 1 with the
     * new `MediaSort` (a local re-sort would only reorder the loaded page). No-op
     * when nothing changes, so re-picking the same row never fires a request.
     */
    override fun changeSort(option: UserListSort, order: UserListOrder) {
        if (option == sortOption && order == sortOrder) return
        sortOption = option
        sortOrder = order
        val id = currentDetailId ?: return
        fetchFirstPage(id, clearDetail = false, clearLists = true)
    }

    fun loadMore() {
        val id = currentDetailId ?: return
        media.loadMore(
            scope = scope,
            isCurrent = { currentDetailId == id },
            fetch = { page -> repository.loadMediaPage(id, page, apiSort()) },
            onLoaded = { pageLoaded(id) },
            onError = { pageError(it) },
        )
    }

    override suspend fun fetchDetail(id: Int): NetworkResult<StudioDetail> =
        repository.loadDetail(id, apiSort())

    override fun onDetailLoaded(detail: StudioDetail) {
        media.set(detail.media)
    }

    override fun cancelPages() {
        media.cancel()
    }

    override fun clearPages() {
        media.clear()
    }

    override fun onCacheMiss(id: Int) {
        // No cached snapshot: a studio always opens on the default order (the
        // user's last selection is per-studio, restored from the cache).
        sortOption = DEFAULT_SORT_OPTION
        sortOrder = DEFAULT_SORT_ORDER
    }

    override fun snapshot(detail: StudioDetail): StudioDetailSnapshot = StudioDetailSnapshot(
        detail = detail,
        media = media.snapshot(),
        sortOption = sortOption,
        sortOrder = sortOrder,
    )

    override fun restore(snapshot: StudioDetailSnapshot) {
        detail = snapshot.detail
        media.set(snapshot.media)
        sortOption = snapshot.sortOption
        sortOrder = snapshot.sortOrder
    }

    override fun withFavourite(detail: StudioDetail, value: Boolean): StudioDetail =
        detail.copy(isFavourite = value)

    override fun snapshotWithFavourite(snapshot: StudioDetailSnapshot, value: Boolean): StudioDetailSnapshot =
        snapshot.copy(detail = snapshot.detail.copy(isFavourite = value))

    override suspend fun requestToggleFavourite(id: Int, detail: StudioDetail): NetworkResult<Unit> =
        repository.toggleFavourite(id)
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
