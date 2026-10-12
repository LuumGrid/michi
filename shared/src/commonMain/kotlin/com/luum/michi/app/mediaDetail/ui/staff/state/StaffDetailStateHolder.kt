package com.luum.michi.app.mediaDetail.ui.staff.state

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaSort
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
    // Snapshot-backed: the detail screen reads these directly and must
    // recompose on every load/pagination (plain vars would apply silently).
    var detail: StaffDetail? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set
    var error: NetworkError? by mutableStateOf(null)
        private set

    private var currentStaffId: Int? = null
    private var currentJob: Job? = null

    private val detailCache = LruCache<Int, StaffDetailSnapshot>(maxSize = 5)

    /**
     * Server page order never changes: the API always fetches by popularity,
     * and [sort] is applied client-side by the screen. This keeps sort changes
     * instant (no re-query, no list reset, no scroll reset).
     */
    private val serverSort = StaffMediaSort.POPULARITY

    // ── media list ──
    var mediaItems: List<StaffMediaItem> by mutableStateOf(emptyList())
        private set
    var mediaHasNextPage: Boolean by mutableStateOf(false)
        private set
    var mediaCurrentPage: Int by mutableStateOf(1)
        private set
    var isLoadingMoreMedia: Boolean by mutableStateOf(false)
        private set
    var sort: StaffMediaSort by mutableStateOf(StaffMediaSort.NEWEST)
        private set

    // ── character list ──
    var characterItems: List<StaffCharacterItem> by mutableStateOf(emptyList())
        private set
    var charactersHasNextPage: Boolean by mutableStateOf(false)
        private set
    var charactersCurrentPage: Int by mutableStateOf(1)
        private set
    var isLoadingMoreCharacters: Boolean by mutableStateOf(false)
        private set

    // ── favourite ──
    var isFavourite: Boolean by mutableStateOf(false)
        private set
    var isTogglingFavourite: Boolean by mutableStateOf(false)
        private set

    fun load(id: Int) {
        if (currentStaffId == id && (detail != null || isLoading)) return

        val cached = detailCache.get(id)
        if (cached != null) {
            currentStaffId = id
            currentJob?.cancel()
            detail = cached.detail
            mediaItems = cached.mediaItems
            mediaHasNextPage = cached.mediaHasNextPage
            mediaCurrentPage = cached.mediaCurrentPage
            sort = cached.sort
            characterItems = cached.characterItems
            charactersHasNextPage = cached.charactersHasNextPage
            charactersCurrentPage = cached.charactersCurrentPage
            isFavourite = cached.detail.isFavourite
            error = null
            isLoading = false
            return
        }

        currentStaffId = id
        currentJob?.cancel()
        detail = null
        error = null
        isLoading = true
        mediaItems = emptyList()
        mediaHasNextPage = false
        mediaCurrentPage = 1
        characterItems = emptyList()
        charactersHasNextPage = false
        charactersCurrentPage = 1

        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, serverSort, strings)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detail = value
                    mediaItems = value.media.items.distinctBy { it.mediaId }
                    mediaHasNextPage = value.media.hasNextPage
                    mediaCurrentPage = value.media.currentPage
                    characterItems = value.characters.items.distinctRoles()
                    charactersHasNextPage = value.characters.hasNextPage
                    charactersCurrentPage = value.characters.currentPage
                    isFavourite = value.isFavourite
                    detailCache.put(id, buildSnapshot(value))
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    fun refresh() {
        val id = currentStaffId ?: return
        // Invalidate cached entry so the page is re-fetched from network.
        detailCache.remove(id)
        currentJob?.cancel()
        isLoading = true
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, serverSort, strings)) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detail = value
                    mediaItems = value.media.items.distinctBy { it.mediaId }
                    mediaHasNextPage = value.media.hasNextPage
                    mediaCurrentPage = value.media.currentPage
                    characterItems = value.characters.items.distinctRoles()
                    charactersHasNextPage = value.characters.hasNextPage
                    charactersCurrentPage = value.characters.currentPage
                    isFavourite = value.isFavourite
                    detailCache.put(id, buildSnapshot(value))
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    /** Loads the next page of the MEDIA (roles) list only. */
    fun loadMoreMedia() {
        if (isLoadingMoreMedia || !mediaHasNextPage) return
        val id = currentStaffId ?: return
        val nextPage = mediaCurrentPage + 1
        scope.launch {
            isLoadingMoreMedia = true
            when (val result = repository.loadMediaPage(id, nextPage, serverSort)) {
                is NetworkResult.Success -> {
                    mediaItems = (mediaItems + result.value.items).distinctBy { it.mediaId }
                    mediaHasNextPage = result.value.hasNextPage
                    mediaCurrentPage = result.value.currentPage
                    detail?.let { d -> detailCache.put(id, buildSnapshot(d)) }
                }
                is NetworkResult.Failure -> error = result.error
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
                    detail?.let { d -> detailCache.put(id, buildSnapshot(d)) }
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoadingMoreCharacters = false
        }
    }

    /**
     * Display-only sort: flips the local order immediately. The loaded media
     * list is never reset and no network call fires (the screen applies
     * [sortedFor] to each role group). Characters pagination is untouched.
     */
    fun changeSort(newSort: StaffMediaSort) {
        if (newSort == sort) return
        sort = newSort
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
                    detail?.let { d ->
                        val updated = d.copy(isFavourite = isFavourite)
                        detail = updated
                        detailCache.put(id, buildSnapshot(updated))
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
