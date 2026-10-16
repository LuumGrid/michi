package com.luum.michi.app.mediaDetail.ui.media.state

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.util.DetailCache
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
    // Read at request launch (not stored): a language change must not recreate
    // the holder and drop its cache, but a new request must use the new strings.
    private val stringsProvider: () -> LanguageStrings,
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
    /** Non-blocking action failure (a page load or a favourite toggle): shown as
     *  a compact row while the content stays visible, never as the load error. */
    var actionError: NetworkError? by mutableStateOf(null)
        private set

    private var currentMediaId: Int? = null
    private var currentJob: Job? = null
    private var charactersJob: Job? = null
    private var staffJob: Job? = null
    private var recommendationsJob: Job? = null

    /** Cache of up to 5 recently-visited detail pages. */
    private val detailCache = DetailCache<Int, MediaDetailSnapshot>(maxSize = 5)

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
            cancelPaginationJobs()
            detail = cached.detail
            characters = cached.characters
            charactersHasNextPage = cached.charactersHasNextPage
            charactersCurrentPage = cached.charactersCurrentPage
            staff = cached.staff
            staffHasNextPage = cached.staffHasNextPage
            staffCurrentPage = cached.staffCurrentPage
            error = null
            actionError = null
            isLoading = false
            isLoadingCharacters = false
            isLoadingStaff = false
            // Reset per-visit transient state
            resetRecommendations()
            return
        }

        currentMediaId = mediaId
        currentJob?.cancel()
        cancelPaginationJobs()
        detail = null
        error = null
        actionError = null
        isLoading = true
        characters = emptyList()
        staff = emptyList()
        charactersHasNextPage = false
        staffHasNextPage = false
        charactersCurrentPage = 1
        staffCurrentPage = 1

        resetRecommendations()

        currentJob = scope.launch {
            when (val result = repository.loadDetail(mediaId, stringsProvider())) {
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
        // Cancelling the pagination jobs would drop the initial recommendations
        // fetch; remember it (or an already-loaded list) so it is relaunched in
        // the new language once the detail answers.
        val hadRecommendations = recommendationsJob?.isActive == true || recommendations.isNotEmpty()
        cancelPaginationJobs()
        error = null
        actionError = null
        isLoading = true
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, stringsProvider())) {
                is NetworkResult.Success -> {
                    detail = result.value
                    characters = result.value.characters.items
                    charactersHasNextPage = result.value.characters.hasNextPage
                    charactersCurrentPage = result.value.characters.currentPage
                    staff = result.value.staff.items
                    staffHasNextPage = result.value.staff.hasNextPage
                    staffCurrentPage = result.value.staff.currentPage
                    cacheSnapshot(id, result.value)
                    if (hadRecommendations && currentMediaId == id) {
                        resetRecommendations()
                        loadRecommendations()
                    }
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
        charactersJob?.cancel()
        charactersJob = scope.launch {
            isLoadingCharacters = true
            when (val result = repository.loadCharactersPage(id, page = nextPage)) {
                is NetworkResult.Success -> if (currentMediaId == id) {
                    characters = characters + result.value.items
                    charactersHasNextPage = result.value.hasNextPage
                    charactersCurrentPage = result.value.currentPage
                    actionError = null
                }
                is NetworkResult.Failure -> if (currentMediaId == id) actionError = result.error
            }
            if (currentMediaId == id) isLoadingCharacters = false
        }
    }

    fun loadMoreStaff() {
        if (isLoadingStaff || !staffHasNextPage) return
        val id = currentMediaId ?: return
        val nextPage = staffCurrentPage + 1
        staffJob?.cancel()
        staffJob = scope.launch {
            isLoadingStaff = true
            when (val result = repository.loadStaffPage(id, page = nextPage)) {
                is NetworkResult.Success -> if (currentMediaId == id) {
                    staff = staff + result.value.items
                    staffHasNextPage = result.value.hasNextPage
                    staffCurrentPage = result.value.currentPage
                    actionError = null
                }
                is NetworkResult.Failure -> if (currentMediaId == id) actionError = result.error
            }
            if (currentMediaId == id) isLoadingStaff = false
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
        val id = current.id
        val target = !current.isFavourite
        isTogglingFavourite = true
        scope.launch {
            when (val result = entryRepository.toggleFavourite(id, current.type == MediaDetailType.MANGA)) {
                is NetworkResult.Success -> {
                    // The cached snapshot of THIS media gets the new value; the
                    // visible state follows only while this holder still shows it
                    // (the user may have opened another media meanwhile, and the
                    // response must never overwrite that one).
                    detailCache.update(id) { snapshot ->
                        snapshot.copy(detail = snapshot.detail.copy(isFavourite = target))
                    }
                    if (currentMediaId == id) detail = detail?.copy(isFavourite = target)
                    actionError = null
                }
                is NetworkResult.Failure -> if (currentMediaId == id) actionError = result.error
            }
            isTogglingFavourite = false
        }
    }

    fun loadRecommendations() {
        if (isLoadingRecommendations || recommendations.isNotEmpty()) return
        val id = currentMediaId ?: return
        recommendationsJob?.cancel()
        recommendationsJob = scope.launch {
            isLoadingRecommendations = true
            when (val result = repository.loadRecommendations(id)) {
                is NetworkResult.Success -> if (currentMediaId == id) {
                    recommendations = result.value.items
                    recommendationsHasNextPage = result.value.hasNextPage
                    recommendationsCurrentPage = result.value.currentPage
                    actionError = null
                }
                is NetworkResult.Failure -> if (currentMediaId == id) actionError = result.error
            }
            if (currentMediaId == id) isLoadingRecommendations = false
        }
    }

    fun loadMoreRecommendations() {
        if (isLoadingRecommendations || !recommendationsHasNextPage) return
        val id = currentMediaId ?: return
        val nextPage = recommendationsCurrentPage + 1
        recommendationsJob?.cancel()
        recommendationsJob = scope.launch {
            isLoadingRecommendations = true
            when (val result = repository.loadRecommendations(id, nextPage)) {
                is NetworkResult.Success -> if (currentMediaId == id) {
                    recommendations = recommendations + result.value.items
                    recommendationsHasNextPage = result.value.hasNextPage
                    recommendationsCurrentPage = result.value.currentPage
                    actionError = null
                }
                is NetworkResult.Failure -> if (currentMediaId == id) actionError = result.error
            }
            if (currentMediaId == id) isLoadingRecommendations = false
        }
    }

    /**
     * Applies a favourite change made from the editor to this media's state and
     * cached snapshot, so closing the editor never leaves the detail stale.
     */
    fun applyFavouriteChange(mediaId: Int, value: Boolean) {
        detailCache.update(mediaId) { snapshot ->
            snapshot.copy(detail = snapshot.detail.copy(isFavourite = value))
        }
        if (currentMediaId == mediaId) detail = detail?.copy(isFavourite = value)
    }

    /** Drops every cached snapshot: a language change re-localizes all of them. */
    fun invalidateCache() {
        detailCache.clear()
    }

    /** Dismisses the compact action-error row. */
    fun clearActionError() {
        actionError = null
    }

    private fun cancelPaginationJobs() {
        charactersJob?.cancel()
        charactersJob = null
        staffJob?.cancel()
        staffJob = null
        recommendationsJob?.cancel()
        recommendationsJob = null
        isLoadingCharacters = false
        isLoadingStaff = false
        isLoadingRecommendations = false
    }

    private fun resetRecommendations() {
        recommendations = emptyList()
        recommendationsHasNextPage = false
        recommendationsCurrentPage = 1
    }

    fun clear() {
        currentJob?.cancel()
        currentJob = null
        cancelPaginationJobs()
        currentMediaId = null
        detail = null
        isLoading = false
        error = null
        actionError = null
        characters = emptyList()
        staff = emptyList()
        charactersHasNextPage = false
        staffHasNextPage = false
        charactersCurrentPage = 1
        staffCurrentPage = 1

        resetRecommendations()
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createMediaDetailStateHolder(
    repository: MediaDetailRepository,
    entryRepository: MediaListEntryRepository,
    scope: CoroutineScope,
    stringsProvider: () -> LanguageStrings,
): MediaDetailStateHolder {
    return MediaDetailStateHolder(repository, entryRepository, scope, stringsProvider)
}
