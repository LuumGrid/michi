package com.luum.michi.app.mediaDetail.ui.media.state

import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.core.domain.language.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
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
    private val scope: CoroutineScope,
    private val strings: LanguageStrings,
) {
    private var detailState: MediaDetail? = null
    private var loadingState = false
    private var errorState: NetworkError? = null
    private var currentMediaId: Int? = null
    private var currentJob: Job? = null

    /** LRU cache of up to 5 recently-visited detail pages. */
    private val detailCache = LruCache<Int, MediaDetailSnapshot>(maxSize = 5)

    var voiceLanguage = "JAPANESE"
        private set
    var characters: List<MediaCharacterEntry> = emptyList()
        private set
    var charactersHasNextPage = false
        private set
    var charactersCurrentPage = 1
        private set
    var isLoadingCharacters = false
        private set

    var staff: List<MediaStaffEntry> = emptyList()
        private set
    var staffHasNextPage = false
        private set
    var staffCurrentPage = 1
        private set
    var isLoadingStaff = false
        private set

    var recommendations: List<MediaRecommendationEntry> = emptyList()
        private set
    var isLoadingRecommendations = false
        private set

    val detail: MediaDetail? get() = detailState
    val isLoading: Boolean get() = loadingState
    val error: NetworkError? get() = errorState

    fun load(mediaId: Int) {
        if (currentMediaId == mediaId && (detailState != null || loadingState)) return

        // Restore from LRU cache if available (avoids a full network round-trip).
        val cached = detailCache.get(mediaId)
        if (cached != null) {
            currentMediaId = mediaId
            currentJob?.cancel()
            detailState = cached.detail
            characters = cached.characters
            charactersHasNextPage = cached.charactersHasNextPage
            charactersCurrentPage = cached.charactersCurrentPage
            staff = cached.staff
            staffHasNextPage = cached.staffHasNextPage
            staffCurrentPage = cached.staffCurrentPage
            errorState = null
            loadingState = false
            // Reset per-visit transient state
            recommendations = emptyList()
            return
        }

        currentMediaId = mediaId
        currentJob?.cancel()
        detailState = null
        errorState = null
        loadingState = true
        characters = emptyList()
        staff = emptyList()
        charactersHasNextPage = false
        staffHasNextPage = false
        charactersCurrentPage = 1
        staffCurrentPage = 1

        recommendations = emptyList()

        currentJob = scope.launch {
            when (val result = repository.loadDetail(mediaId, voiceLanguage, strings)) {
                is NetworkResult.Success -> {
                    detailState = result.value
                    characters = result.value.characters.items
                    charactersHasNextPage = result.value.characters.hasNextPage
                    charactersCurrentPage = result.value.characters.currentPage
                    staff = result.value.staff.items
                    staffHasNextPage = result.value.staff.hasNextPage
                    staffCurrentPage = result.value.staff.currentPage
                    // Store in LRU cache for fast back-navigation
                    detailCache.put(
                        mediaId,
                        MediaDetailSnapshot(
                            detail = result.value,
                            characters = characters,
                            charactersHasNextPage = charactersHasNextPage,
                            charactersCurrentPage = charactersCurrentPage,
                            staff = staff,
                            staffHasNextPage = staffHasNextPage,
                            staffCurrentPage = staffCurrentPage,
                        ),
                    )
                }
                is NetworkResult.Failure -> errorState = result.error
            }
            loadingState = false
        }
    }

    fun refresh() {
        val id = currentMediaId ?: return
        // Invalidate cached entry so next load() re-fetches from network.
        detailCache.remove(id)
        currentJob?.cancel()
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, voiceLanguage, strings)) {
                is NetworkResult.Success -> {
                    detailState = result.value
                    characters = result.value.characters.items
                    charactersHasNextPage = result.value.characters.hasNextPage
                    charactersCurrentPage = result.value.characters.currentPage
                    staff = result.value.staff.items
                    staffHasNextPage = result.value.staff.hasNextPage
                    staffCurrentPage = result.value.staff.currentPage
                    detailCache.put(
                        id,
                        MediaDetailSnapshot(
                            detail = result.value,
                            characters = characters,
                            charactersHasNextPage = charactersHasNextPage,
                            charactersCurrentPage = charactersCurrentPage,
                            staff = staff,
                            staffHasNextPage = staffHasNextPage,
                            staffCurrentPage = staffCurrentPage,
                        ),
                    )
                }
                is NetworkResult.Failure -> errorState = result.error
            }
        }
    }

    fun selectVoiceLanguage(language: String) {
        if (language == voiceLanguage) return
        voiceLanguage = language
        val id = currentMediaId ?: return
        scope.launch {
            isLoadingCharacters = true
            when (val result = repository.loadCharactersPage(id, page = 1, voiceLanguage = language)) {
                is NetworkResult.Success -> {
                    characters = result.value.items
                    charactersHasNextPage = result.value.hasNextPage
                    charactersCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error
            }
            isLoadingCharacters = false
        }
    }

    fun loadMoreCharacters() {
        if (isLoadingCharacters || !charactersHasNextPage) return
        val id = currentMediaId ?: return
        val nextPage = charactersCurrentPage + 1
        scope.launch {
            isLoadingCharacters = true
            when (val result = repository.loadCharactersPage(id, page = nextPage, voiceLanguage = voiceLanguage)) {
                is NetworkResult.Success -> {
                    characters = characters + result.value.items
                    charactersHasNextPage = result.value.hasNextPage
                    charactersCurrentPage = result.value.currentPage
                }
                is NetworkResult.Failure -> errorState = result.error
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
                is NetworkResult.Failure -> errorState = result.error
            }
            isLoadingStaff = false
        }
    }

    fun loadRecommendations() {
        if (isLoadingRecommendations || recommendations.isNotEmpty()) return
        val id = currentMediaId ?: return
        scope.launch {
            isLoadingRecommendations = true
            when (val result = repository.loadRecommendations(id)) {
                is NetworkResult.Success -> {
                    recommendations = result.value
                }
                is NetworkResult.Failure -> errorState = result.error
            }
            isLoadingRecommendations = false
        }
    }

    fun clear() {
        currentJob?.cancel()
        currentJob = null
        currentMediaId = null
        detailState = null
        loadingState = false
        errorState = null
        characters = emptyList()
        staff = emptyList()
        charactersHasNextPage = false
        staffHasNextPage = false
        charactersCurrentPage = 1
        staffCurrentPage = 1

        recommendations = emptyList()
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createMediaDetailStateHolder(
    repository: MediaDetailRepository,
    scope: CoroutineScope,
    strings: LanguageStrings,
): MediaDetailStateHolder {
    return MediaDetailStateHolder(repository, scope, strings)
}
