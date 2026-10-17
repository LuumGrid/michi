package com.luum.michi.app.mediaDetail.ui.staff.state

import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.navigation.domain.DetailHolder
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.util.DetailCache
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaItem
import com.luum.michi.app.ui.state.PagedList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Cache snapshot holds BOTH paginated lists independently so that a page
 * loaded on media-only doesn't evict the already-loaded character list and
 * vice versa.
 */
private data class StaffDetailSnapshot(
    val detail: StaffDetail,
    val media: Page<StaffMediaItem>,
    val characters: Page<StaffCharacterItem>,
)

internal class StaffDetailStateHolder(
    private val repository: StaffDetailRepository,
    private val scope: CoroutineScope,
    // Read at request launch: a language change keeps the holder (and its cache).
    private val stringsProvider: () -> LanguageStrings,
) : DetailHolder {
    // Snapshot-backed: the detail screen reads these directly and must
    // recompose on every load/pagination (plain vars would apply silently).
    var detail: StaffDetail? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set
    var error: NetworkError? by mutableStateOf(null)
        private set
    /** Non-blocking action failure (page load / favourite), shown as a compact row. */
    var actionError: NetworkError? by mutableStateOf(null)
        private set

    private var currentStaffId: Int? = null
    private var currentJob: Job? = null

    private val detailCache = DetailCache<Int, StaffDetailSnapshot>(maxSize = 5)

    // ── media (roles) list ──
    // The same work can repeat with several roles (Director + Storyboard), so
    // rows are deduped by (work, role), not by work alone.
    val media = PagedList<StaffMediaItem> { a, b -> (a + b).distinctBy { it.mediaId to it.staffRole } }

    // ── character list ──
    // The same character in two works is two rows: dedupe by (character, work).
    val characters = PagedList<StaffCharacterItem> { a, b -> (a + b).distinctBy { it.characterId to it.mediaId } }

    // ── favourite ──
    /** Derived from the visible detail: no separate flag can drift from it. */
    override val isFavourite: Boolean get() = detail?.isFavourite == true

    // Per id: a toggle in flight for one staff member must not disable (or
    // reflect on) another one's heart.
    private var favouriteInFlightId: Int? by mutableStateOf(null)
    val isTogglingFavourite: Boolean
        get() = favouriteInFlightId != null && favouriteInFlightId == currentStaffId

    fun load(id: Int) {
        if (currentStaffId == id && (detail != null || isLoading)) return

        val cached = detailCache.get(id)
        if (cached != null) {
            currentStaffId = id
            currentJob?.cancel()
            // Pages in flight belong to what was on screen: never append them to
            // the restored snapshot (set cancels them).
            detail = cached.detail
            media.set(cached.media)
            characters.set(cached.characters)
            error = null
            actionError = null
            isLoading = false
            return
        }

        currentStaffId = id
        fetchFirstPage(id, clearDetail = true, clearMedia = true)
    }

    override fun refresh() {
        val id = currentStaffId ?: return
        // Invalidate cached entry so the page is re-fetched from network.
        detailCache.remove(id)
        // Keeps the detail and both lists on screen while reloading (no
        // flicker); only a new staff member clears them.
        fetchFirstPage(id, clearDetail = false, clearMedia = false)
    }

    /** Loads the next page of the MEDIA (roles) list only. */
    fun loadMoreMedia() {
        val id = currentStaffId ?: return
        media.loadMore(
            scope = scope,
            isCurrent = { currentStaffId == id },
            fetch = { page -> repository.loadMediaPage(id, page) },
            onLoaded = {
                actionError = null
                cacheSnapshot(id)
            },
            onError = { actionError = it },
        )
    }

    /** Loads the next page of the CHARACTERS list only. */
    fun loadMoreCharacters() {
        val id = currentStaffId ?: return
        characters.loadMore(
            scope = scope,
            isCurrent = { currentStaffId == id },
            fetch = { page -> repository.loadCharacterPage(id, page) },
            onLoaded = {
                actionError = null
                cacheSnapshot(id)
            },
            onError = { actionError = it },
        )
    }

    /** Fetches page 1; the single network path of the holder. */
    private fun fetchFirstPage(id: Int, clearDetail: Boolean, clearMedia: Boolean) {
        currentJob?.cancel()
        // Pages from the previous staff member must never append.
        media.cancel()
        characters.cancel()
        if (clearDetail) detail = null
        if (clearMedia) {
            media.clear()
            characters.clear()
        }
        error = null
        actionError = null
        isLoading = true
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, stringsProvider())) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detail = value
                    media.set(value.media)
                    characters.set(value.characters)
                    cacheSnapshot(id)
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    override fun toggleFavourite() {
        val id = currentStaffId ?: return
        val current = detail ?: return
        if (favouriteInFlightId == id) return
        val previous = current.isFavourite
        val target = !previous
        favouriteInFlightId = id
        // Optimistic: the visible detail and its cached snapshot both flip now,
        // so a revisit from the cache never restores the stale value.
        detail = current.copy(isFavourite = target)
        detailCache.update(id) { snapshot ->
            snapshot.copy(detail = snapshot.detail.copy(isFavourite = target))
        }
        scope.launch {
            when (val result = repository.toggleFavourite(id)) {
                is NetworkResult.Success -> {
                    detailCache.update(id) { snapshot ->
                        snapshot.copy(detail = snapshot.detail.copy(isFavourite = target))
                    }
                    if (currentStaffId == id) detail = detail?.copy(isFavourite = target)
                }
                is NetworkResult.Failure -> {
                    // Revert both copies of THIS staff member's value; the visible
                    // page and its error only while it is still the current one.
                    detailCache.update(id) { snapshot ->
                        snapshot.copy(detail = snapshot.detail.copy(isFavourite = previous))
                    }
                    if (currentStaffId == id) {
                        detail = detail?.copy(isFavourite = previous)
                        actionError = result.error
                    }
                }
            }
            if (favouriteInFlightId == id) favouriteInFlightId = null
        }
    }

    /** Drops every cached snapshot: a language change re-localizes all of them. */
    override fun invalidateCache() {
        detailCache.clear()
    }

    /** Dismisses the compact action-error row. */
    fun clearActionError() {
        actionError = null
    }

    private fun cacheSnapshot(id: Int) {
        val value = detail ?: return
        detailCache.put(
            id,
            StaffDetailSnapshot(
                detail = value,
                media = media.snapshot(),
                characters = characters.snapshot(),
            ),
        )
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createStaffDetailStateHolder(
    repository: StaffDetailRepository,
    scope: CoroutineScope,
    stringsProvider: () -> LanguageStrings,
): StaffDetailStateHolder {
    return StaffDetailStateHolder(repository, scope, stringsProvider)
}
