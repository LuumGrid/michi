package com.luum.michi.app.mediaDetail.ui.character.state

import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaSort
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.language.domain.LanguageStrings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

private data class CharacterDetailSnapshot(
    val detail: CharacterDetail,
    val mediaItems: List<CharacterMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
    val sort: CharacterMediaSort,
)

internal class CharacterDetailStateHolder(
    private val repository: CharacterDetailRepository,
    private val scope: CoroutineScope,
    private val strings: LanguageStrings,
    @Suppress("UNUSED_PARAMETER") viewerId: Int,
) {
    // Snapshot-backed: the detail screen reads these directly and must
    // recompose on every load/pagination (plain vars would apply silently).
    var detail: CharacterDetail? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set
    var error: NetworkError? by mutableStateOf(null)
        private set

    private var currentCharacterId: Int? = null
    private var currentJob: Job? = null

    private val detailCache = LruCache<Int, CharacterDetailSnapshot>(maxSize = 5)

    /**
     * Server page order never changes: the API always fetches by popularity,
     * and [sort] is applied client-side by the screen. This keeps sort changes
     * instant (no re-query, no list reset, no scroll reset).
     */
    private val serverSort = CharacterMediaSort.POPULARITY

    var mediaItems: List<CharacterMediaItem> by mutableStateOf(emptyList())
        private set
    var hasNextPage: Boolean by mutableStateOf(false)
        private set
    var currentPage: Int by mutableStateOf(1)
        private set
    var isLoadingMore: Boolean by mutableStateOf(false)
        private set
    var sort: CharacterMediaSort by mutableStateOf(CharacterMediaSort.NEWEST)
        private set
    var isFavourite: Boolean by mutableStateOf(false)
        private set
    var isTogglingFavourite: Boolean by mutableStateOf(false)
        private set

    fun load(id: Int) {
        if (currentCharacterId == id && (detail != null || isLoading)) return

        val cached = detailCache.get(id)
        if (cached != null) {
            currentCharacterId = id
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

        currentCharacterId = id
        currentJob?.cancel()
        detail = null
        error = null
        isLoading = true
        mediaItems = emptyList()
        hasNextPage = false
        currentPage = 1
        isLoadingMore = false

        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, serverSort, strings)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detail = value
                    mediaItems = value.media.items
                    hasNextPage = value.media.hasNextPage
                    currentPage = value.media.currentPage
                    isFavourite = value.isFavourite
                    detailCache.put(
                        id,
                        CharacterDetailSnapshot(
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
        val id = currentCharacterId ?: return
        // Invalidate cached entry so the page is re-fetched from network.
        detailCache.remove(id)
        currentJob?.cancel()
        isLoading = true
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, serverSort, strings)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detail = value
                    mediaItems = value.media.items
                    hasNextPage = value.media.hasNextPage
                    currentPage = value.media.currentPage
                    isFavourite = value.isFavourite
                    detailCache.put(
                        id,
                        CharacterDetailSnapshot(
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
        val id = currentCharacterId ?: return
        val nextPage = currentPage + 1
        scope.launch {
            isLoadingMore = true
            when (val result = repository.loadMediaPage(id, nextPage, serverSort)) {
                is NetworkResult.Success -> {
                    mediaItems = mediaItems + result.value.items
                    hasNextPage = result.value.hasNextPage
                    currentPage = result.value.currentPage
                    detail?.let { d ->
                        detailCache.put(
                            id,
                            CharacterDetailSnapshot(
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
    fun changeSort(newSort: CharacterMediaSort) {
        if (newSort == sort) return
        sort = newSort
    }

    fun toggleFavourite() {
        if (isTogglingFavourite) return
        val id = currentCharacterId ?: return
        val previous = isFavourite
        isFavourite = !previous
        isTogglingFavourite = true
        scope.launch {
            when (val result = repository.toggleFavourite(id)) {
                is NetworkResult.Success -> {
                    detail?.let { d ->
                        val updated = d.copy(isFavourite = isFavourite)
                        detail = updated
                        detailCache.put(
                            id,
                            CharacterDetailSnapshot(
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
internal fun createCharacterDetailStateHolder(
    repository: CharacterDetailRepository,
    scope: CoroutineScope,
    strings: LanguageStrings,
    viewerId: Int,
): CharacterDetailStateHolder {
    return CharacterDetailStateHolder(repository, scope, strings, viewerId)
}
