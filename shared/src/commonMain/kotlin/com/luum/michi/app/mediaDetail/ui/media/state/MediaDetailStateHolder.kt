package com.luum.michi.app.mediaDetail.ui.media.state

import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.navigation.domain.DetailHolder
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
import com.luum.michi.app.ui.state.PagedList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Snapshot of MediaDetail content that can be restored without a network call. */
private data class MediaDetailSnapshot(
    val detail: MediaDetail,
    val characters: Page<MediaCharacterEntry>,
    val staff: Page<MediaStaffEntry>,
)

internal class MediaDetailStateHolder(
    private val repository: MediaDetailRepository,
    private val entryRepository: MediaListEntryRepository,
    private val scope: CoroutineScope,
    // Read at request launch (not stored): a language change must not recreate
    // the holder and drop its cache, but a new request must use the new strings.
    private val stringsProvider: () -> LanguageStrings,
) : DetailHolder {
    // Snapshot-backed: the detail screen reads these directly and must
    // recompose on every load/pagination (plain vars would apply silently).
    var detail: MediaDetail? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set
    var error: NetworkError? by mutableStateOf(null)
        private set
    // Per id: a toggle in flight for one media must not disable (or reflect on)
    // another one's heart.
    private var favouriteInFlightId: Int? by mutableStateOf(null)
    val isTogglingFavourite: Boolean
        get() = favouriteInFlightId != null && favouriteInFlightId == currentMediaId
    /** Non-blocking action failure (a page load or a favourite toggle): shown as
     *  a compact row while the content stays visible, never as the load error. */
    var actionError: NetworkError? by mutableStateOf(null)
        private set

    private var currentMediaId: Int? = null
    private var currentJob: Job? = null

    /** Cache of up to 5 recently-visited detail pages. */
    private val detailCache = DetailCache<Int, MediaDetailSnapshot>(maxSize = 5)

    /** Characters grid; a character is one row per media (dedup by id). */
    val characters = PagedList<MediaCharacterEntry> { a, b -> (a + b).distinctBy { it.characterId } }

    /** Staff grid; a person can repeat with a different role (dedup by id + role). */
    val staff = PagedList<MediaStaffEntry> { a, b -> (a + b).distinctBy { it.staffId to it.role } }

    /**
     * Recommendations (per visit; not part of the cache snapshot). Loaded on
     * demand the first time and paged from there.
     */
    val recommendations = PagedList<MediaRecommendationEntry> { a, b -> (a + b).distinctBy { it.id } }

    fun load(mediaId: Int) {
        if (currentMediaId == mediaId && (detail != null || isLoading)) return

        // Restore from LRU cache if available (avoids a full network round-trip).
        val cached = detailCache.get(mediaId)
        if (cached != null) {
            currentMediaId = mediaId
            currentJob?.cancel()
            // Pages in flight belong to what was on screen: never append them to
            // the restored snapshot (set cancels them).
            detail = cached.detail
            characters.set(cached.characters)
            staff.set(cached.staff)
            error = null
            actionError = null
            isLoading = false
            // Reset per-visit transient state.
            recommendations.clear()
            return
        }

        currentMediaId = mediaId
        currentJob?.cancel()
        detail = null
        error = null
        actionError = null
        isLoading = true
        characters.clear()
        staff.clear()
        recommendations.clear()

        currentJob = scope.launch {
            when (val result = repository.loadDetail(mediaId, stringsProvider())) {
                is NetworkResult.Success -> {
                    detail = result.value
                    characters.set(result.value.characters)
                    staff.set(result.value.staff)
                    // Store in LRU cache for fast back-navigation
                    cacheSnapshot(mediaId)
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    override fun refresh() {
        val id = currentMediaId ?: return
        // Invalidate cached entry so next load() re-fetches from network.
        detailCache.remove(id)
        currentJob?.cancel()
        // Cancelling the pagination would drop the initial recommendations
        // fetch; remember it (or an already-loaded list) so it is relaunched in
        // the new language once the detail answers.
        val hadRecommendations = recommendations.isLoadingMore || recommendations.items.isNotEmpty()
        characters.cancel()
        staff.cancel()
        recommendations.cancel()
        error = null
        actionError = null
        isLoading = true
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, stringsProvider())) {
                is NetworkResult.Success -> {
                    detail = result.value
                    characters.set(result.value.characters)
                    staff.set(result.value.staff)
                    cacheSnapshot(id)
                    if (hadRecommendations && currentMediaId == id) {
                        recommendations.clear()
                        loadRecommendations()
                    }
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    fun loadMoreCharacters() {
        val id = currentMediaId ?: return
        characters.loadMore(
            scope = scope,
            isCurrent = { currentMediaId == id },
            fetch = { page -> repository.loadCharactersPage(id, page = page) },
            onLoaded = {
                actionError = null
                cacheSnapshot(id)
            },
            onError = { actionError = it },
        )
    }

    fun loadMoreStaff() {
        val id = currentMediaId ?: return
        staff.loadMore(
            scope = scope,
            isCurrent = { currentMediaId == id },
            fetch = { page -> repository.loadStaffPage(id, page = page) },
            onLoaded = {
                actionError = null
                cacheSnapshot(id)
            },
            onError = { actionError = it },
        )
    }

    private fun cacheSnapshot(mediaId: Int) {
        val value = detail ?: return
        detailCache.put(
            mediaId,
            MediaDetailSnapshot(
                detail = value,
                characters = characters.snapshot(),
                staff = staff.snapshot(),
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

    /** The media keeps its flag on the detail snapshot (see [DetailHolder]). */
    override val isFavourite: Boolean get() = detail?.isFavourite == true

    override fun toggleFavourite() {
        val current = detail ?: return
        val id = current.id
        if (favouriteInFlightId == id) return
        val target = !current.isFavourite
        favouriteInFlightId = id
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
            if (favouriteInFlightId == id) favouriteInFlightId = null
        }
    }

    /** Ensures the recommendations first page is loaded (no-op once it has items). */
    fun loadRecommendations() {
        if (recommendations.items.isNotEmpty()) return
        val id = currentMediaId ?: return
        recommendations.loadFirst(
            scope = scope,
            isCurrent = { currentMediaId == id },
            fetch = { repository.loadRecommendations(id) },
            onError = { actionError = it },
        )
    }

    fun loadMoreRecommendations() {
        val id = currentMediaId ?: return
        recommendations.loadMore(
            scope = scope,
            isCurrent = { currentMediaId == id },
            fetch = { page -> repository.loadRecommendations(id, page) },
            onError = { actionError = it },
        )
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
    override fun invalidateCache() {
        detailCache.clear()
    }

    /** Dismisses the compact action-error row. */
    fun clearActionError() {
        actionError = null
    }

    fun clear() {
        currentJob?.cancel()
        currentJob = null
        characters.clear()
        staff.clear()
        recommendations.clear()
        currentMediaId = null
        detail = null
        isLoading = false
        error = null
        actionError = null
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
