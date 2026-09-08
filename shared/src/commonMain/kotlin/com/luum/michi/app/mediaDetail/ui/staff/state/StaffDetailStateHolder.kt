package com.luum.michi.app.mediaDetail.ui.staff.state

import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.core.domain.language.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaSort
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
    private val strings: LanguageStrings,
    @Suppress("UNUSED_PARAMETER") viewerId: Int,
) {
    private var detailState: StaffDetail? = null
    private var loadingState = false
    private var errorState: NetworkError? = null
    private var currentStaffId: Int? = null
    private var currentJob: Job? = null

    private val detailCache = LruCache<Int, StaffDetailSnapshot>(maxSize = 5)

    // ── media list ──
    var mediaItems: List<StaffMediaItem> = emptyList()
        private set
    var mediaHasNextPage = false
        private set
    var mediaCurrentPage = 1
        private set
    var isLoadingMoreMedia = false
        private set
    var sort = StaffMediaSort.NEWEST
        private set

    // ── character list ──
    var characterItems: List<StaffCharacterItem> = emptyList()
        private set
    var charactersHasNextPage = false
        private set
    var charactersCurrentPage = 1
        private set
    var isLoadingMoreCharacters = false
        private set

    // ── favourite ──
    var isFavourite = false
        private set
    var isTogglingFavourite = false
        private set

    val detail: StaffDetail? get() = detailState
    val isLoading: Boolean get() = loadingState
    val error: NetworkError? get() = errorState

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
            when (val result = repository.loadDetail(id, sort, strings)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detailState = value
                    mediaItems = value.media.items.distinctBy { it.mediaId }
                    mediaHasNextPage = value.media.hasNextPage
                    mediaCurrentPage = value.media.currentPage
                    characterItems = value.characters.items.distinctRoles()
                    charactersHasNextPage = value.characters.hasNextPage
                    charactersCurrentPage = value.characters.currentPage
                    isFavourite = value.isFavourite
                    detailCache.put(id, buildSnapshot(value))
                }
                is NetworkResult.Failure -> errorState = result.error
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
                    mediaItems = (mediaItems + result.value.items).distinctBy { it.mediaId }
                    mediaHasNextPage = result.value.hasNextPage
                    mediaCurrentPage = result.value.currentPage
                    detailState?.let { d -> detailCache.put(id, buildSnapshot(d)) }
                }
                is NetworkResult.Failure -> errorState = result.error
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
                    characterItems = (characterItems + result.value.items).distinctRoles()
                    charactersHasNextPage = result.value.hasNextPage
                    charactersCurrentPage = result.value.currentPage
                    detailState?.let { d -> detailCache.put(id, buildSnapshot(d)) }
                }
                is NetworkResult.Failure -> errorState = result.error
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
            when (val result = repository.loadDetail(id, newSort, strings)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detailState = value
                    mediaItems = value.media.items.distinctBy { it.mediaId }
                    mediaHasNextPage = value.media.hasNextPage
                    mediaCurrentPage = value.media.currentPage
                    // Preserve the already-loaded character list across a sort change.
                    // Only seed from the fresh response if nothing has been loaded yet.
                    if (characterItems.isEmpty()) {
                        characterItems = value.characters.items.distinctRoles()
                        charactersHasNextPage = value.characters.hasNextPage
                        charactersCurrentPage = value.characters.currentPage
                    }
                    isFavourite = value.isFavourite
                    detailCache.put(id, buildSnapshot(value))
                }
                is NetworkResult.Failure -> errorState = result.error
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
                    errorState = result.error
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

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createStaffDetailStateHolder(
    repository: StaffDetailRepository,
    scope: CoroutineScope,
    strings: LanguageStrings,
    viewerId: Int,
): StaffDetailStateHolder {
    return StaffDetailStateHolder(repository, scope, strings, viewerId)
}

/** Roles son únicos por (personaje, anime): mismo personaje en dos anime son dos filas. */
private fun List<StaffCharacterItem>.distinctRoles(): List<StaffCharacterItem> =
    distinctBy { "${it.characterId}_${it.mediaTitle}" }

