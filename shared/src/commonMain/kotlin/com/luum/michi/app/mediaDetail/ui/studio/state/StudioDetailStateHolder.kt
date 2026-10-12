package com.luum.michi.app.mediaDetail.ui.studio.state

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaItem
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaSort
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * LRU cache reused from the MediaDetail pattern.
 * Evicts the least-recently-used entry when maxSize is exceeded.
 */
private class LruCache<K, V>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>()

    fun get(key: K): V? = map[key]

    fun put(key: K, value: V) {
        map.remove(key)
        map[key] = value
        if (map.size > maxSize) {
            val oldestKey = map.keys.first()
            map.remove(oldestKey)
        }
    }

    fun remove(key: K) { map.remove(key) }
}

/** Snapshot stored in the LRU cache for fast back-navigation. */
private data class StudioDetailSnapshot(
    val detail: StudioDetail,
    val mediaItems: List<StudioMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
    val sort: StudioMediaSort,
)

internal class StudioDetailStateHolder(
    private val repository: StudioDetailRepository,
    private val scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER") viewerId: Int,
) {
    // Snapshot-backed: the detail screen reads these directly and must
    // recompose on every load/pagination (plain vars would apply silently).
    var detail: StudioDetail? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set
    var error: NetworkError? by mutableStateOf(null)
        private set

    private var currentStudioId: Int? = null
    private var currentJob: Job? = null

    private val detailCache = LruCache<Int, StudioDetailSnapshot>(maxSize = 5)

    /**
     * Server page order never changes: the API always fetches by popularity,
     * and [sort] is applied client-side by the screen. This keeps sort changes
     * instant (no re-query, no list reset, no scroll reset).
     */
    private val serverSort = StudioMediaSort.POPULARITY

    var mediaItems: List<StudioMediaItem> by mutableStateOf(emptyList())
        private set
    var hasNextPage: Boolean by mutableStateOf(false)
        private set
    var currentPage: Int by mutableStateOf(1)
        private set
    var isLoadingMore: Boolean by mutableStateOf(false)
        private set
    var sort: StudioMediaSort by mutableStateOf(StudioMediaSort.NEWEST)
        private set
    var isFavourite: Boolean by mutableStateOf(false)
        private set
    var isTogglingFavourite: Boolean by mutableStateOf(false)
        private set

    fun load(id: Int) {
        if (currentStudioId == id && (detail != null || isLoading)) return

        val cached = detailCache.get(id)
        if (cached != null) {
            currentStudioId = id
            currentJob?.cancel()
            detail = cached.detail
            mediaItems = cached.mediaItems
            hasNextPage = cached.hasNextPage
            currentPage = cached.currentPage
            sort = cached.sort
            isFavourite = cached.detail.isFavourite
            error = null
            isLoading = false
            return
        }

        currentStudioId = id
        currentJob?.cancel()
        detail = null
        error = null
        isLoading = true
        mediaItems = emptyList()
        hasNextPage = false
        currentPage = 1
        isLoadingMore = false

        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, serverSort)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detail = value
                    mediaItems = value.media.items
                    hasNextPage = value.media.hasNextPage
                    currentPage = value.media.currentPage
                    isFavourite = value.isFavourite
                    detailCache.put(
                        id,
                        StudioDetailSnapshot(
                            detail = value,
                            mediaItems = mediaItems,
                            hasNextPage = hasNextPage,
                            currentPage = currentPage,
                            sort = sort,
                        ),
                    )
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    fun refresh() {
        val id = currentStudioId ?: return
        // Invalidate cached entry so the page is re-fetched from network.
        detailCache.remove(id)
        currentJob?.cancel()
        isLoading = true
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, serverSort)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detail = value
                    mediaItems = value.media.items
                    hasNextPage = value.media.hasNextPage
                    currentPage = value.media.currentPage
                    isFavourite = value.isFavourite
                    detailCache.put(
                        id,
                        StudioDetailSnapshot(
                            detail = value,
                            mediaItems = mediaItems,
                            hasNextPage = hasNextPage,
                            currentPage = currentPage,
                            sort = sort,
                        ),
                    )
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasNextPage) return
        val id = currentStudioId ?: return
        val nextPage = currentPage + 1
        scope.launch {
            isLoadingMore = true
            when (val result = repository.loadMediaPage(id, nextPage, serverSort)) {
                is NetworkResult.Success -> {
                    mediaItems = mediaItems + result.value.items
                    hasNextPage = result.value.hasNextPage
                    currentPage = result.value.currentPage
                    // Update cache with new page data
                    detail?.let { d ->
                        detailCache.put(
                            id,
                            StudioDetailSnapshot(
                                detail = d,
                                mediaItems = mediaItems,
                                hasNextPage = hasNextPage,
                                currentPage = currentPage,
                                sort = sort,
                            ),
                        )
                    }
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoadingMore = false
        }
    }

    /**
     * Display-only sort: flips the local order immediately. The loaded list is
     * never reset and no network call fires (the screen applies [sortedFor]).
     */
    fun changeSort(newSort: StudioMediaSort) {
        if (newSort == sort) return
        sort = newSort
    }

    fun toggleFavourite() {
        if (isTogglingFavourite) return
        val id = currentStudioId ?: return
        val previous = isFavourite
        isFavourite = !previous
        isTogglingFavourite = true
        scope.launch {
            when (val result = repository.toggleFavourite(id)) {
                is NetworkResult.Success -> {
                    // Keep the cached snapshot in sync so back-navigation shows
                    // the new favourite state instead of the pre-toggle value.
                    detail?.let { d ->
                        val updated = d.copy(isFavourite = isFavourite)
                        detail = updated
                        detailCache.put(
                            id,
                            StudioDetailSnapshot(
                                detail = updated,
                                mediaItems = mediaItems,
                                hasNextPage = hasNextPage,
                                currentPage = currentPage,
                                sort = sort,
                            ),
                        )
                    }
                }
                is NetworkResult.Failure -> {
                    isFavourite = previous
                    error = result.error
                }
            }
            isTogglingFavourite = false
        }
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createStudioDetailStateHolder(
    repository: StudioDetailRepository,
    scope: CoroutineScope,
    viewerId: Int,
): StudioDetailStateHolder {
    return StudioDetailStateHolder(repository, scope, viewerId)
}
