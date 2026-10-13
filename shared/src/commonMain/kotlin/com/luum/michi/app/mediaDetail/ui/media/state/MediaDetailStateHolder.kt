package com.luum.michi.app.mediaDetail.ui.media.state

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Tiny LRU cache that evicts the least-recently-used entry when [maxSize] is exceeded.
 * Insertion-order eviction is used as a simple approximation of LRU (sufficient for 5 entries).
 * Uses a plain [LinkedHashMap] in insertion order (no access-order flag for KMP compat).
 */
private class LruCache<K, V>(private val maxSize: Int) {
    // Access order is not available in Kotlin/Native LinkedHashMap; use insertion-order
    // and treat "put" as a touch (remove + re-insert) to approximate LRU.
    private val map = LinkedHashMap<K, V>()

    fun get(key: K): V? = map[key]

    fun put(key: K, value: V) {
        // Re-insert to move to "most recently used" position (insertion order approximation)
        map.remove(key)
        map[key] = value
        if (map.size > maxSize) {
            val oldestKey = map.keys.first()
            map.remove(oldestKey)
        }
    }

    fun remove(key: K) { map.remove(key) }
}

/** Snapshot of MediaDetail content that can be restored without a network call. */
private data class MediaDetailSnapshot(
    val detail: MediaDetail,
    val characters: List<MediaCharacterEntry>,
    val charactersHasNextPage: Boolean,
    val charactersCurrentPage: Int,
    val staff: List<MediaStaffEntry>,
    val staffHasNextPage: Boolean,
    val staffCurrentPage: Int,
)

internal class MediaDetailStateHolder(
    private val repository: MediaDetailRepository,
    private val entryRepository: MediaListEntryRepository,
    private val scope: CoroutineScope,
    private val strings: LanguageStrings,
) {
    // Snapshot-backed: the detail screen reads these directly and must
    // recompose on every load/pagination (plain vars would apply silently).
    var detail: MediaDetail? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set
    var error: NetworkError? by mutableStateOf(null)
        private set
    var isTogglingFavourite: Boolean by mutableStateOf(false)
        private set

    private var currentMediaId: Int? = null
    private var currentJob: Job? = null

    /** LRU cache of up to 5 recently-visited detail pages. */
    private val detailCache = LruCache<Int, MediaDetailSnapshot>(maxSize = 5)

    var characters: List<MediaCharacterEntry> by mutableStateOf(emptyList())
        private set
    var charactersHasNextPage: Boolean by mutableStateOf(false)
        private set
    var charactersCurrentPage: Int by mutableStateOf(1)
        private set
    var isLoadingCharacters: Boolean by mutableStateOf(false)
        private set

    var staff: List<MediaStaffEntry> by mutableStateOf(emptyList())
        private set
    var staffHasNextPage: Boolean by mutableStateOf(false)
        private set
    var staffCurrentPage: Int by mutableStateOf(1)
        private set
    var isLoadingStaff: Boolean by mutableStateOf(false)
        private set

    var recommendations: List<MediaRecommendationEntry> by mutableStateOf(emptyList())
        private set
    var recommendationsHasNextPage: Boolean by mutableStateOf(false)
        private set
    var recommendationsCurrentPage: Int by mutableStateOf(1)
        private set
    var isLoadingRecommendations: Boolean by mutableStateOf(false)
        private set

    fun load(mediaId: Int) {
        if (currentMediaId == mediaId && (detail != null || isLoading)) return

        // Restore from LRU cache if available (avoids a full network round-trip).
        val cached = detailCache.get(mediaId)
        if (cached != null) {
            currentMediaId = mediaId
            currentJob?.cancel()
            detail = cached.detail
            characters = cached.characters
            charactersHasNextPage = cached.charactersHasNextPage
            charactersCurrentPage = cached.charactersCurrentPage
            staff = cached.staff
            staffHasNextPage = cached.staffHasNextPage
            staffCurrentPage = cached.staffCurrentPage
            error = null
            isLoading = false
            isLoadingCharacters = false
            isLoadingStaff = false
            // Reset per-visit transient state
            recommendations = emptyList()
            return
        }

        currentMediaId = mediaId
        currentJob?.cancel()
        detail = null
        error = null
        isLoading = true
        characters = emptyList()
        staff = emptyList()
        charactersHasNextPage = false
        staffHasNextPage = false
        charactersCurrentPage = 1
        staffCurrentPage = 1

        recommendations = emptyList()

        currentJob = scope.launch {
            when (val result = repository.loadDetail(mediaId, strings)) {
                is NetworkResult.Success -> {
                    detail = result.value
                    characters = result.value.characters.items
                    charactersHasNextPage = result.value.characters.hasNextPage
                    charactersCurrentPage = result.value.characters.currentPage
                    staff = result.value.staff.items
                    staffHasNextPage = result.value.staff.hasNextPage
                    staffCurrentPage = result.value.staff.currentPage
                    // Store in LRU cache for fast back-navigation
                    cacheSnapshot(mediaId, result.value)
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    fun refresh() {
        val id = currentMediaId ?: return
        // Invalidate cached entry so next load() re-fetches from network.
        detailCache.remove(id)
        currentJob?.cancel()
        isLoading = true
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, strings)) {
                is NetworkResult.Success -> {
                    detail = result.value
                    characters = result.value.characters.items
                    charactersHasNextPage = result.value.characters.hasNextPage
                    charactersCurrentPage = result.value.characters.currentPage
                    staff = result.value.staff.items
                    staffHasNextPage = result.value.staff.hasNextPage
                    staffCurrentPage = result.value.staff.currentPage
                    cacheSnapshot(id, result.value)
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    fun loadMoreCharacters() {
        if (isLoadingCharacters || !charactersHasNextPage) return
        val id = currentMediaId ?: return
        val nextPage = charactersCurrentPage + 1
        scope.launch {
            isLoadingCharacters = true
            when (val result = repository.loadCharactersPage(id, page = nextPage)) {
                is NetworkResult.Success -> {
                    characters = characters + result.value.items
                    charactersHasNextPage = result.value.hasNextPage
                    charactersCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoadingCharacters = false
        }
    }

    fun loadMoreStaff() {
        if (isLoadingStaff || !staffHasNextPage) return
        val id = currentMediaId ?: return
        val nextPage = staffCurrentPage + 1
        scope.launch {
            isLoadingStaff = true
            when (val result = repository.loadStaffPage(id, page = nextPage)) {
                is NetworkResult.Success -> {
                    staff = staff + result.value.items
                    staffHasNextPage = result.value.hasNextPage
                    staffCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoadingStaff = false
        }
    }

    private fun cacheSnapshot(mediaId: Int, value: MediaDetail) {
        detailCache.put(
            mediaId,
            MediaDetailSnapshot(
                detail = value,
                characters = characters,
                charactersHasNextPage = charactersHasNextPage,
                charactersCurrentPage = charactersCurrentPage,
                staff = staff,
                staffHasNextPage = staffHasNextPage,
                staffCurrentPage = staffCurrentPage,
            ),
        )
    }

    /**
     * The editor saved or deleted the viewer's entry for [mediaId] (from any
     * surface): drop the cached snapshot so a later visit never restores the
     * old entry, and re-fetch when this holder still shows that media so the
     * entry card is not stale.
     */
    fun onEntryChanged(mediaId: Int) {
        detailCache.remove(mediaId)
        if (currentMediaId == mediaId && detail != null) refresh()
    }

    fun toggleFavourite() {
        val current = detail ?: return
        if (isTogglingFavourite) return
        isTogglingFavourite = true
        scope.launch {
            when (val result = entryRepository.toggleFavourite(current.id, current.type == MediaDetailType.MANGA)) {
                is NetworkResult.Success -> {
                    val latest = detail
                    if (latest != null) detail = latest.copy(isFavourite = !latest.isFavourite)
                }
                is NetworkResult.Failure -> error = result.error
            }
            isTogglingFavourite = false
        }
    }

    fun loadRecommendations() {
        if (isLoadingRecommendations || recommendations.isNotEmpty()) return
        val id = currentMediaId ?: return
        scope.launch {
            isLoadingRecommendations = true
            when (val result = repository.loadRecommendations(id)) {
                is NetworkResult.Success -> {
                    recommendations = result.value.items
                    recommendationsHasNextPage = result.value.hasNextPage
                    recommendationsCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoadingRecommendations = false
        }
    }

    fun loadMoreRecommendations() {
        if (isLoadingRecommendations || !recommendationsHasNextPage) return
        val id = currentMediaId ?: return
        val nextPage = recommendationsCurrentPage + 1
        scope.launch {
            isLoadingRecommendations = true
            when (val result = repository.loadRecommendations(id, nextPage)) {
                is NetworkResult.Success -> {
                    recommendations = recommendations + result.value.items
                    recommendationsHasNextPage = result.value.hasNextPage
                    recommendationsCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoadingRecommendations = false
        }
    }

    fun clear() {
        currentJob?.cancel()
        currentJob = null
        currentMediaId = null
        detail = null
        isLoading = false
        error = null
        characters = emptyList()
        staff = emptyList()
        charactersHasNextPage = false
        staffHasNextPage = false
        charactersCurrentPage = 1
        staffCurrentPage = 1

        recommendations = emptyList()
        recommendationsHasNextPage = false
        recommendationsCurrentPage = 1
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createMediaDetailStateHolder(
    repository: MediaDetailRepository,
    entryRepository: MediaListEntryRepository,
    scope: CoroutineScope,
    strings: LanguageStrings,
): MediaDetailStateHolder {
    return MediaDetailStateHolder(repository, entryRepository, scope, strings)
}
