package com.luum.michi.app.studioDetail.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.studioDetail.data.StudioDetailRepository
import com.luum.michi.app.studioDetail.presentation.model.StudioDetail
import com.luum.michi.app.studioDetail.presentation.model.StudioMediaItem
import com.luum.michi.app.studioDetail.presentation.model.StudioMediaSort
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
    private var detailState by mutableStateOf<StudioDetail?>(null)
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    private var currentStudioId: Int? = null
    private var currentJob: Job? = null

    private val detailCache = LruCache<Int, StudioDetailSnapshot>(maxSize = 5)

    var mediaItems by mutableStateOf<List<StudioMediaItem>>(emptyList())
        private set
    var hasNextPage by mutableStateOf(false)
        private set
    var currentPage by mutableStateOf(1)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var sort by mutableStateOf(StudioMediaSort.POPULARITY)
        private set
    var isFavourite by mutableStateOf(false)
        private set
    var isTogglingFavourite by mutableStateOf(false)
        private set

    val detail: StudioDetail? get() = detailState
    val isLoading: Boolean get() = loadingState
    val error: NetworkError? get() = errorState

    fun load(id: Int) {
        if (currentStudioId == id && (detailState != null || loadingState)) return

        val cached = detailCache.get(id)
        if (cached != null) {
            currentStudioId = id
            currentJob?.cancel()
            detailState = cached.detail
            mediaItems = cached.mediaItems
            hasNextPage = cached.hasNextPage
            currentPage = cached.currentPage
            sort = cached.sort
            isFavourite = cached.detail.isFavourite
            errorState = null
            loadingState = false
            return
        }

        currentStudioId = id
        currentJob?.cancel()
        detailState = null
        errorState = null
        loadingState = true
        mediaItems = emptyList()
        hasNextPage = false
        currentPage = 1
        isLoadingMore = false

        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, sort)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detailState = value
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
                is NetworkResult.Failure -> errorState = result.error
            }
            loadingState = false
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasNextPage) return
        val id = currentStudioId ?: return
        val nextPage = currentPage + 1
        scope.launch {
            isLoadingMore = true
            when (val result = repository.loadMediaPage(id, nextPage, sort)) {
                is NetworkResult.Success -> {
                    mediaItems = mediaItems + result.value.items
                    hasNextPage = result.value.hasNextPage
                    currentPage = result.value.currentPage
                    // Update cache with new page data
                    detailState?.let { d ->
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
                is NetworkResult.Failure -> errorState = result.error
            }
            isLoadingMore = false
        }
    }

    fun changeSort(newSort: StudioMediaSort) {
        if (newSort == sort) return
        sort = newSort
        val id = currentStudioId ?: return
        // Invalidate cache for this studio so the new sort is fetched fresh
        detailCache.remove(id)
        currentJob?.cancel()
        mediaItems = emptyList()
        hasNextPage = false
        currentPage = 1
        errorState = null
        loadingState = true

        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, newSort)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detailState = value
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
                            sort = newSort,
                        ),
                    )
                }
                is NetworkResult.Failure -> errorState = result.error
            }
            loadingState = false
        }
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
                    detailState?.let { d ->
                        val updated = d.copy(isFavourite = isFavourite)
                        detailState = updated
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
                    errorState = result.error
                }
            }
            isTogglingFavourite = false
        }
    }
}

@Composable
internal fun rememberStudioDetailStateHolder(
    repository: StudioDetailRepository,
    viewerId: Int,
): StudioDetailStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository, viewerId) { StudioDetailStateHolder(repository, scope, viewerId) }
}
