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
            detail = cached.detail
            mediaItems = cached.mediaItems
            mediaHasNextPage = cached.mediaHasNextPage
            mediaCurrentPage = cached.mediaCurrentPage
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
            when (val result = repository.loadDetail(id, strings)) {
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
            when (val result = repository.loadDetail(id, strings)) {
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
            when (val result = repository.loadMediaPage(id, nextPage)) {
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
