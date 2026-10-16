package com.luum.michi.app.mediaDetail.ui.staff.state

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.util.DetailCache
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaItem
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
    // ── media list state ──
    val mediaItems: List<StaffMediaItem>,
    val mediaHasNextPage: Boolean,
    val mediaCurrentPage: Int,
    // ── character list state ──
    val characterItems: List<StaffCharacterItem>,
    val charactersHasNextPage: Boolean,
    val charactersCurrentPage: Int,
)

internal class StaffDetailStateHolder(
    private val repository: StaffDetailRepository,
    private val scope: CoroutineScope,
    // Read at request launch: a language change keeps the holder (and its cache).
    private val stringsProvider: () -> LanguageStrings,
) {
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
    // Page-in-flight jobs (one per list), cancelled when the first page is
    // (re)fetched so a page from the previous staff member can never append.
    private var moreMediaJob: Job? = null
    private var moreCharactersJob: Job? = null

    private val detailCache = DetailCache<Int, StaffDetailSnapshot>(maxSize = 5)

    // ── media list ──
    var mediaItems: List<StaffMediaItem> by mutableStateOf(emptyList())
        private set
    var mediaHasNextPage: Boolean by mutableStateOf(false)
        private set
    var mediaCurrentPage: Int by mutableStateOf(1)
        private set
    var isLoadingMoreMedia: Boolean by mutableStateOf(false)
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
            // Pages in flight belong to what was on screen: never append them to
            // the restored snapshot, and drop their spinners (a cancelled
            // coroutine never reaches its own `isLoadingMore = false`).
            moreMediaJob?.cancel()
            moreCharactersJob?.cancel()
            detail = cached.detail
            mediaItems = cached.mediaItems
            mediaHasNextPage = cached.mediaHasNextPage
            mediaCurrentPage = cached.mediaCurrentPage
            characterItems = cached.characterItems
            charactersHasNextPage = cached.charactersHasNextPage
            charactersCurrentPage = cached.charactersCurrentPage
            isFavourite = cached.detail.isFavourite
            error = null
            actionError = null
            isLoading = false
            isLoadingMoreMedia = false
            isLoadingMoreCharacters = false
            return
        }

        currentStaffId = id
        fetchFirstPage(id, clearDetail = true, clearMedia = true)
    }

    fun refresh() {
        val id = currentStaffId ?: return
        // Invalidate cached entry so the page is re-fetched from network.
        detailCache.remove(id)
        // Keeps the detail and both lists on screen while reloading (no
        // flicker); only a new staff member clears them.
        fetchFirstPage(id, clearDetail = false, clearMedia = false)
    }

    /** Loads the next page of the MEDIA (roles) list only. */
    fun loadMoreMedia() {
        if (isLoadingMoreMedia || !mediaHasNextPage) return
        val id = currentStaffId ?: return
        val nextPage = mediaCurrentPage + 1
        moreMediaJob = scope.launch {
            isLoadingMoreMedia = true
            when (val result = repository.loadMediaPage(id, nextPage)) {
                is NetworkResult.Success -> if (currentStaffId == id) {
                    mediaItems = (mediaItems + result.value.items).distinctByRoles()
                    mediaHasNextPage = result.value.hasNextPage
                    mediaCurrentPage = result.value.currentPage
                    detail?.let { cacheSnapshot(id, it) }
                    actionError = null
                }
                is NetworkResult.Failure -> if (currentStaffId == id) actionError = result.error
            }
            if (currentStaffId == id) isLoadingMoreMedia = false
        }
    }

    /** Loads the next page of the CHARACTERS list only. */
    fun loadMoreCharacters() {
        if (isLoadingMoreCharacters || !charactersHasNextPage) return
        val id = currentStaffId ?: return
        val nextPage = charactersCurrentPage + 1
        moreCharactersJob = scope.launch {
            isLoadingMoreCharacters = true
            when (val result = repository.loadCharacterPage(id, nextPage)) {
                is NetworkResult.Success -> if (currentStaffId == id) {
                    characterItems = (characterItems + result.value.items).distinctRoles()
                    charactersHasNextPage = result.value.hasNextPage
                    charactersCurrentPage = result.value.currentPage
                    detail?.let { cacheSnapshot(id, it) }
                    actionError = null
                }
                is NetworkResult.Failure -> if (currentStaffId == id) actionError = result.error
            }
            if (currentStaffId == id) isLoadingMoreCharacters = false
        }
    }

    /** Fetches page 1; the single network path of the holder. */
    private fun fetchFirstPage(id: Int, clearDetail: Boolean, clearMedia: Boolean) {
        currentJob?.cancel()
        // Pages from the previous staff member must never append.
        moreMediaJob?.cancel()
        moreCharactersJob?.cancel()
        if (clearDetail) detail = null
        if (clearMedia) {
            mediaItems = emptyList()
            mediaHasNextPage = false
            mediaCurrentPage = 1
            characterItems = emptyList()
            charactersHasNextPage = false
            charactersCurrentPage = 1
        }
        error = null
        actionError = null
        isLoading = true
        isLoadingMoreMedia = false
        isLoadingMoreCharacters = false
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, stringsProvider())) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detail = value
                    mediaItems = value.media.items.distinctByRoles()
                    mediaHasNextPage = value.media.hasNextPage
                    mediaCurrentPage = value.media.currentPage
                    characterItems = value.characters.items.distinctRoles()
                    charactersHasNextPage = value.characters.hasNextPage
                    charactersCurrentPage = value.characters.currentPage
                    isFavourite = value.isFavourite
                    cacheSnapshot(id, value)
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    fun toggleFavourite() {
        if (isTogglingFavourite) return
        val id = currentStaffId ?: return
        val previous = isFavourite
        val target = !previous
        isFavourite = target
        isTogglingFavourite = true
        scope.launch {
            when (val result = repository.toggleFavourite(id)) {
                is NetworkResult.Success -> {
                    // The cached snapshot of THIS staff member gets the new
                    // value; the visible state follows only while this holder
                    // still shows it (the user may have opened another one).
                    detailCache.update(id) { snapshot ->
                        snapshot.copy(detail = snapshot.detail.copy(isFavourite = target))
                    }
                    if (currentStaffId == id) detail = detail?.copy(isFavourite = target)
                }
                is NetworkResult.Failure -> if (currentStaffId == id) {
                    isFavourite = previous
                    actionError = result.error
                }
            }
            isTogglingFavourite = false
        }
    }

    /** Drops every cached snapshot: a language change re-localizes all of them. */
    fun invalidateCache() {
        detailCache.clear()
    }

    /** Dismisses the compact action-error row. */
    fun clearActionError() {
        actionError = null
    }

    private fun cacheSnapshot(id: Int, d: StaffDetail) = detailCache.put(
        id,
        StaffDetailSnapshot(
            detail = d,
            mediaItems = mediaItems,
            mediaHasNextPage = mediaHasNextPage,
            mediaCurrentPage = mediaCurrentPage,
            characterItems = characterItems,
            charactersHasNextPage = charactersHasNextPage,
            charactersCurrentPage = charactersCurrentPage,
        ),
    )
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createStaffDetailStateHolder(
    repository: StaffDetailRepository,
    scope: CoroutineScope,
    stringsProvider: () -> LanguageStrings,
): StaffDetailStateHolder {
    return StaffDetailStateHolder(repository, scope, stringsProvider)
}

/**
 * Roles de una obra: el mismo anime puede repetir con varios cargos (Director +
 * Storyboard), así que se deduplica por (obra, cargo), no solo por obra.
 */
private fun List<StaffMediaItem>.distinctByRoles(): List<StaffMediaItem> =
    distinctBy { it.mediaId to it.staffRole }

/** Mismo personaje en dos obras son dos filas: dedupe por (personaje, obra). */
private fun List<StaffCharacterItem>.distinctRoles(): List<StaffCharacterItem> =
    distinctBy { it.characterId to it.mediaId }
