package com.luum.michi.app.staffDetail.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.staffDetail.data.StaffDetailRepository
import com.luum.michi.app.staffDetail.presentation.model.StaffCharacterItem
import com.luum.michi.app.staffDetail.presentation.model.StaffDetail
import com.luum.michi.app.staffDetail.presentation.model.StaffMediaItem
import com.luum.michi.app.staffDetail.presentation.model.StaffMediaSort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

/**
 * Cache snapshot holds BOTH paginated lists independently so that sort changes
 * on media-only don't evict the already-loaded character list and vice versa.
 */
private data class StaffDetailSnapshot(
    val detail: StaffDetail,
    // ── media list state ──
    val mediaItems: List<StaffMediaItem>,
    val mediaHasNextPage: Boolean,
    val mediaCurrentPage: Int,
    val sort: StaffMediaSort,
    // ── character list state ──
    val characterItems: List<StaffCharacterItem>,
    val charactersHasNextPage: Boolean,
    val charactersCurrentPage: Int,
)

internal class StaffDetailStateHolder(
    private val repository: StaffDetailRepository,
    private val scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER") viewerId: Int,
) {
    private var detailState by mutableStateOf<StaffDetail?>(null)
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)
    private var currentStaffId: Int? = null
    private var currentJob: Job? = null

    private val detailCache = LruCache<Int, StaffDetailSnapshot>(maxSize = 5)

    // ── media list ──
    var mediaItems by mutableStateOf<List<StaffMediaItem>>(emptyList())
        private set
    var mediaHasNextPage by mutableStateOf(false)
        private set
    var mediaCurrentPage by mutableStateOf(1)
        private set
    var isLoadingMoreMedia by mutableStateOf(false)
        private set
    var sort by mutableStateOf(StaffMediaSort.NEWEST)
        private set

    // ── character list ──
    var characterItems by mutableStateOf<List<StaffCharacterItem>>(emptyList())
        private set
    var charactersHasNextPage by mutableStateOf(false)
        private set
    var charactersCurrentPage by mutableStateOf(1)
        private set
    var isLoadingMoreCharacters by mutableStateOf(false)
        private set

    // ── favourite ──
    var isFavourite by mutableStateOf(false)
        private set
    var isTogglingFavourite by mutableStateOf(false)
        private set

    val detail: StaffDetail? get() = detailState
    val isLoading: Boolean get() = loadingState
    val error: String? get() = errorState

    fun load(id: Int) {
        if (currentStaffId == id && (detailState != null || loadingState)) return

        val cached = detailCache.get(id)
        if (cached != null) {
            currentStaffId = id
            currentJob?.cancel()
            detailState = cached.detail
            mediaItems = cached.mediaItems
            mediaHasNextPage = cached.mediaHasNextPage
            mediaCurrentPage = cached.mediaCurrentPage
            sort = cached.sort
            characterItems = cached.characterItems
            charactersHasNextPage = cached.charactersHasNextPage
            charactersCurrentPage = cached.charactersCurrentPage
            isFavourite = cached.detail.isFavourite
            errorState = null
            loadingState = false
            return
        }

        currentStaffId = id
        currentJob?.cancel()
        detailState = null
        errorState = null
        loadingState = true
        mediaItems = emptyList()
        mediaHasNextPage = false
        mediaCurrentPage = 1
        characterItems = emptyList()
        charactersHasNextPage = false
        charactersCurrentPage = 1

        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, sort)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detailState = value
                    mediaItems = value.media.items
                    mediaHasNextPage = value.media.hasNextPage
                    mediaCurrentPage = value.media.currentPage
                    characterItems = value.characters.items
                    charactersHasNextPage = value.characters.hasNextPage
                    charactersCurrentPage = value.characters.currentPage
                    isFavourite = value.isFavourite
                    detailCache.put(id, buildSnapshot(value))
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            loadingState = false
        }
    }

    /** Loads the next page of the MEDIA (roles) list only. */
    fun loadMoreMedia() {
        if (isLoadingMoreMedia || !mediaHasNextPage) return
        val id = currentStaffId ?: return
        val nextPage = mediaCurrentPage + 1
        scope.launch {
            isLoadingMoreMedia = true
            when (val result = repository.loadMediaPage(id, nextPage, sort)) {
                is NetworkResult.Success -> {
                    mediaItems = mediaItems + result.value.items
                    mediaHasNextPage = result.value.hasNextPage
                    mediaCurrentPage = result.value.currentPage
                    detailState?.let { d -> detailCache.put(id, buildSnapshot(d)) }
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingMoreMedia = false
        }
    }

    /** Loads the next page of the CHARACTERS list only. */
    fun loadMoreCharacters() {
        if (isLoadingMoreCharacters || !charactersHasNextPage) return
        val id = currentStaffId ?: return
        val nextPage = charactersCurrentPage + 1
        scope.launch {
            isLoadingMoreCharacters = true
            when (val result = repository.loadCharacterPage(id, nextPage)) {
                is NetworkResult.Success -> {
                    characterItems = characterItems + result.value.items
                    charactersHasNextPage = result.value.hasNextPage
                    charactersCurrentPage = result.value.currentPage
                    detailState?.let { d -> detailCache.put(id, buildSnapshot(d)) }
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            isLoadingMoreCharacters = false
        }
    }

    /**
     * Changes the sort for the MEDIA list only.
     * Characters pagination is NOT touched — it keeps its own independent state.
     */
    fun changeSort(newSort: StaffMediaSort) {
        if (newSort == sort) return
        sort = newSort
        val id = currentStaffId ?: return
        // Invalidate only the media portion; don't wipe the character list from cache
        detailCache.remove(id)
        currentJob?.cancel()
        mediaItems = emptyList()
        mediaHasNextPage = false
        mediaCurrentPage = 1
        errorState = null
        loadingState = true

        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, newSort)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detailState = value
                    mediaItems = value.media.items
                    mediaHasNextPage = value.media.hasNextPage
                    mediaCurrentPage = value.media.currentPage
                    // Preserve the already-loaded character list across a sort change.
                    // Only seed from the fresh response if nothing has been loaded yet.
                    if (characterItems.isEmpty()) {
                        characterItems = value.characters.items
                        charactersHasNextPage = value.characters.hasNextPage
                        charactersCurrentPage = value.characters.currentPage
                    }
                    isFavourite = value.isFavourite
                    detailCache.put(id, buildSnapshot(value))
                }
                is NetworkResult.Failure -> errorState = result.error.toString()
            }
            loadingState = false
        }
    }

    fun toggleFavourite() {
        if (isTogglingFavourite) return
        val id = currentStaffId ?: return
        val previous = isFavourite
        isFavourite = !previous
        isTogglingFavourite = true
        scope.launch {
            when (val result = repository.toggleFavourite(id)) {
                is NetworkResult.Success -> {
                    detailState?.let { d ->
                        val updated = d.copy(isFavourite = isFavourite)
                        detailState = updated
                        detailCache.put(id, buildSnapshot(updated))
                    }
                }
                is NetworkResult.Failure -> {
                    isFavourite = previous
                    errorState = result.error.toString()
                }
            }
            isTogglingFavourite = false
        }
    }

    private fun buildSnapshot(d: StaffDetail) = StaffDetailSnapshot(
        detail = d,
        mediaItems = mediaItems,
        mediaHasNextPage = mediaHasNextPage,
        mediaCurrentPage = mediaCurrentPage,
        sort = sort,
        characterItems = characterItems,
        charactersHasNextPage = charactersHasNextPage,
        charactersCurrentPage = charactersCurrentPage,
    )
}

@Composable
internal fun rememberStaffDetailStateHolder(
    repository: StaffDetailRepository,
    viewerId: Int,
): StaffDetailStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository, viewerId) { StaffDetailStateHolder(repository, scope, viewerId) }
}
