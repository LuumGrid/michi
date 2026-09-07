package com.luum.michi.app.mediaDetail.ui.character.state

import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaSort
import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.core.domain.language.LanguageStrings
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
    private var detailState: CharacterDetail? = null
    private var loadingState = false
    private var errorState: NetworkError? = null
    private var currentCharacterId: Int? = null
    private var currentJob: Job? = null

    private val detailCache = LruCache<Pair<Int, LanguageStrings>, CharacterDetailSnapshot>(maxSize = 5)

    var mediaItems: List<CharacterMediaItem> = emptyList()
        private set
    var hasNextPage = false
        private set
    var currentPage = 1
        private set
    var isLoadingMore = false
        private set
    var sort = CharacterMediaSort.POPULARITY
        private set
    var isFavourite = false
        private set
    var isTogglingFavourite = false
        private set

    val detail: CharacterDetail? get() = detailState
    val isLoading: Boolean get() = loadingState
    val error: NetworkError? get() = errorState

    fun load(id: Int) {
        if (currentCharacterId == id && (detailState != null || loadingState)) return

        val cached = detailCache.get(id to strings)
        if (cached != null) {
            currentCharacterId = id
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

        currentCharacterId = id
        currentJob?.cancel()
        detailState = null
        errorState = null
        loadingState = true
        mediaItems = emptyList()
        hasNextPage = false
        currentPage = 1
        isLoadingMore = false

        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, sort, strings)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detailState = value
                    mediaItems = value.media.items
                    hasNextPage = value.media.hasNextPage
                    currentPage = value.media.currentPage
                    isFavourite = value.isFavourite
                    detailCache.put(
                        id to strings,
                        CharacterDetailSnapshot(
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
        val id = currentCharacterId ?: return
        val nextPage = currentPage + 1
        scope.launch {
            isLoadingMore = true
            when (val result = repository.loadMediaPage(id, nextPage, sort)) {
                is NetworkResult.Success -> {
                    mediaItems = mediaItems + result.value.items
                    hasNextPage = result.value.hasNextPage
                    currentPage = result.value.currentPage
                    detailState?.let { d ->
                        detailCache.put(
                            id to strings,
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
                is NetworkResult.Failure -> errorState = result.error
            }
            isLoadingMore = false
        }
    }

    fun changeSort(newSort: CharacterMediaSort) {
        if (newSort == sort) return
        sort = newSort
        val id = currentCharacterId ?: return
        detailCache.remove(id to strings)
        currentJob?.cancel()
        mediaItems = emptyList()
        hasNextPage = false
        currentPage = 1
        errorState = null
        loadingState = true

        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, newSort, strings)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detailState = value
                    mediaItems = value.media.items
                    hasNextPage = value.media.hasNextPage
                    currentPage = value.media.currentPage
                    isFavourite = value.isFavourite
                    detailCache.put(
                        id to strings,
                        CharacterDetailSnapshot(
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
        val id = currentCharacterId ?: return
        val previous = isFavourite
        isFavourite = !previous
        isTogglingFavourite = true
        scope.launch {
            when (val result = repository.toggleFavourite(id)) {
                is NetworkResult.Success -> {
                    detailState?.let { d ->
                        val updated = d.copy(isFavourite = isFavourite)
                        detailState = updated
                        detailCache.put(
                            id to strings,
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
                    errorState = result.error
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
