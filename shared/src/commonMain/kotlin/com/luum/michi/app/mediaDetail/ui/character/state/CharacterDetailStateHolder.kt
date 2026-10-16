package com.luum.michi.app.mediaDetail.ui.character.state

import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.util.DetailCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class CharacterDetailSnapshot(
    val detail: CharacterDetail,
    val mediaItems: List<CharacterMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
    val voiceLanguage: String,
)

internal class CharacterDetailStateHolder(
    private val repository: CharacterDetailRepository,
    private val scope: CoroutineScope,
    // Read at request launch: a language change keeps the holder (and its cache).
    private val stringsProvider: () -> LanguageStrings,
) {
    // Snapshot-backed: the detail screen reads these directly and must
    // recompose on every load/pagination (plain vars would apply silently).
    var detail: CharacterDetail? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set
    var error: NetworkError? by mutableStateOf(null)
        private set
    /** Non-blocking action failure (page load / favourite), shown as a compact row. */
    var actionError: NetworkError? by mutableStateOf(null)
        private set

    private var currentCharacterId: Int? = null
    private var currentJob: Job? = null
    // Page-in-flight job, cancelled when the first page is (re)fetched so a page
    // from the previous character/language can never append to the new list.
    private var moreJob: Job? = null

    private val detailCache = DetailCache<Int, CharacterDetailSnapshot>(maxSize = 5)

    var mediaItems: List<CharacterMediaItem> by mutableStateOf(emptyList())
        private set
    var hasNextPage: Boolean by mutableStateOf(false)
        private set
    var currentPage: Int by mutableStateOf(1)
        private set
    var isLoadingMore: Boolean by mutableStateOf(false)
        private set
    /** Voice-actor language for the media edges; changing it re-fetches. */
    var voiceLanguage: String by mutableStateOf("JAPANESE")
        private set
    var isFavourite: Boolean by mutableStateOf(false)
        private set
    var isTogglingFavourite: Boolean by mutableStateOf(false)
        private set

    fun load(id: Int) {
        if (currentCharacterId == id && (detail != null || isLoading)) return

        val cached = detailCache.get(id)
        if (cached != null && cached.voiceLanguage == voiceLanguage) {
            currentCharacterId = id
            currentJob?.cancel()
            // A page in flight belongs to what was on screen: never append it to
            // the restored snapshot, and drop its spinner (a cancelled coroutine
            // never reaches its own `isLoadingMore = false`).
            moreJob?.cancel()
            detail = cached.detail
            mediaItems = cached.mediaItems
            hasNextPage = cached.hasNextPage
            currentPage = cached.currentPage
            isFavourite = cached.detail.isFavourite
            error = null
            actionError = null
            isLoading = false
            isLoadingMore = false
            return
        }

        currentCharacterId = id
        fetchFirstPage(id, clearDetail = true, clearMedia = true)
    }

    fun refresh() {
        val id = currentCharacterId ?: return
        // Invalidate cached entry so the page is re-fetched from network.
        detailCache.remove(id)
        // Keeps the detail and the list on screen while reloading (no flicker);
        // only a new character clears them.
        fetchFirstPage(id, clearDetail = false, clearMedia = false)
    }

    fun loadMore() {
        if (isLoadingMore || !hasNextPage) return
        val id = currentCharacterId ?: return
        val nextPage = currentPage + 1
        moreJob = scope.launch {
            isLoadingMore = true
            when (val result = repository.loadMediaPage(id, nextPage, voiceLanguage)) {
                is NetworkResult.Success -> if (currentCharacterId == id) {
                    mediaItems = (mediaItems + result.value.items).distinctBy { it.mediaId }
                    hasNextPage = result.value.hasNextPage
                    currentPage = result.value.currentPage
                    detail?.let { cacheSnapshot(id, it) }
                    actionError = null
                }
                is NetworkResult.Failure -> if (currentCharacterId == id) actionError = result.error
            }
            if (currentCharacterId == id) isLoadingMore = false
        }
    }

    /** Fetches page 1; the single network path of the holder. */
    private fun fetchFirstPage(id: Int, clearDetail: Boolean, clearMedia: Boolean) {
        currentJob?.cancel()
        // A page from the previous character/language must never append.
        moreJob?.cancel()
        if (clearDetail) detail = null
        if (clearMedia) {
            mediaItems = emptyList()
            hasNextPage = false
            currentPage = 1
        }
        error = null
        actionError = null
        isLoading = true
        isLoadingMore = false
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, voiceLanguage, stringsProvider())) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detail = value
                    mediaItems = value.media.items
                    hasNextPage = value.media.hasNextPage
                    currentPage = value.media.currentPage
                    isFavourite = value.isFavourite
                    cacheSnapshot(id, value)
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    private fun cacheSnapshot(id: Int, detail: CharacterDetail) {
        detailCache.put(
            id,
            CharacterDetailSnapshot(
                detail = detail,
                mediaItems = mediaItems,
                hasNextPage = hasNextPage,
                currentPage = currentPage,
                voiceLanguage = voiceLanguage,
            ),
        )
    }

    /** Voice-actor language change: drop the cache and re-fetch with the new cast. */
    fun selectVoiceLanguage(language: String) {
        if (language == voiceLanguage) return
        voiceLanguage = language
        refresh()
    }

    fun toggleFavourite() {
        if (isTogglingFavourite) return
        val id = currentCharacterId ?: return
        val previous = isFavourite
        val target = !previous
        isFavourite = target
        isTogglingFavourite = true
        scope.launch {
            when (val result = repository.toggleFavourite(id)) {
                is NetworkResult.Success -> {
                    // The cached snapshot of THIS character gets the new value;
                    // the visible state follows only while this holder still
                    // shows it (the user may have opened another one meanwhile).
                    detailCache.update(id) { snapshot ->
                        snapshot.copy(detail = snapshot.detail.copy(isFavourite = target))
                    }
                    if (currentCharacterId == id) detail = detail?.copy(isFavourite = target)
                }
                is NetworkResult.Failure -> if (currentCharacterId == id) {
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
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createCharacterDetailStateHolder(
    repository: CharacterDetailRepository,
    scope: CoroutineScope,
    stringsProvider: () -> LanguageStrings,
): CharacterDetailStateHolder {
    return CharacterDetailStateHolder(repository, scope, stringsProvider)
}
