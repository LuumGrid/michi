package com.luum.michi.app.mediaDetail.ui.character.state

import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.navigation.domain.DetailHolder
import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.util.DetailCache
import com.luum.michi.app.ui.state.PagedList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class CharacterDetailSnapshot(
    val detail: CharacterDetail,
    val media: Page<CharacterMediaItem>,
    val voiceLanguage: String,
)

internal class CharacterDetailStateHolder(
    private val repository: CharacterDetailRepository,
    private val scope: CoroutineScope,
    // Read at request launch: a language change keeps the holder (and its cache).
    private val stringsProvider: () -> LanguageStrings,
) : DetailHolder {
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

    private val detailCache = DetailCache<Int, CharacterDetailSnapshot>(maxSize = 5)

    /** Voice-actor language for the media edges; changing it re-fetches. */
    var voiceLanguage: String by mutableStateOf("JAPANESE")
        private set

    /** Appearances grid (one row per media). */
    val media = PagedList<CharacterMediaItem> { a, b -> (a + b).distinctBy { it.mediaId } }

    /** Derived from the visible detail: no separate flag can drift from it. */
    override val isFavourite: Boolean get() = detail?.isFavourite == true

    // Per id: a toggle in flight for one character must not disable (or reflect
    // on) another one's heart.
    private var favouriteInFlightId: Int? by mutableStateOf(null)
    val isTogglingFavourite: Boolean
        get() = favouriteInFlightId != null && favouriteInFlightId == currentCharacterId

    fun load(id: Int) {
        if (currentCharacterId == id && (detail != null || isLoading)) return

        val cached = detailCache.get(id)
        if (cached != null && cached.voiceLanguage == voiceLanguage) {
            currentCharacterId = id
            currentJob?.cancel()
            // A page in flight belongs to what was on screen: never append it to
            // the restored snapshot (set cancels it).
            detail = cached.detail
            media.set(cached.media)
            error = null
            actionError = null
            isLoading = false
            return
        }

        currentCharacterId = id
        fetchFirstPage(id, clearDetail = true, clearMedia = true)
    }

    override fun refresh() {
        val id = currentCharacterId ?: return
        // Invalidate cached entry so the page is re-fetched from network.
        detailCache.remove(id)
        // Keeps the detail and the list on screen while reloading (no flicker);
        // only a new character clears them.
        fetchFirstPage(id, clearDetail = false, clearMedia = false)
    }

    fun loadMore() {
        val id = currentCharacterId ?: return
        media.loadMore(
            scope = scope,
            isCurrent = { currentCharacterId == id },
            fetch = { page -> repository.loadMediaPage(id, page, voiceLanguage) },
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
        // A page from the previous character/language must never append.
        media.cancel()
        if (clearDetail) detail = null
        if (clearMedia) media.clear()
        error = null
        actionError = null
        isLoading = true
        currentJob = scope.launch {
            when (val result = repository.loadDetail(id, voiceLanguage, stringsProvider())) {
                is NetworkResult.Success -> {
                    val value = result.value
                    detail = value
                    media.set(value.media)
                    cacheSnapshot(id)
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    private fun cacheSnapshot(id: Int) {
        val value = detail ?: return
        detailCache.put(
            id,
            CharacterDetailSnapshot(
                detail = value,
                media = media.snapshot(),
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

    override fun toggleFavourite() {
        val id = currentCharacterId ?: return
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
                    if (currentCharacterId == id) detail = detail?.copy(isFavourite = target)
                }
                is NetworkResult.Failure -> {
                    // Revert both copies of THIS character's value; the visible
                    // page and its error only while it is still the current one.
                    detailCache.update(id) { snapshot ->
                        snapshot.copy(detail = snapshot.detail.copy(isFavourite = previous))
                    }
                    if (currentCharacterId == id) {
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
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createCharacterDetailStateHolder(
    repository: CharacterDetailRepository,
    scope: CoroutineScope,
    stringsProvider: () -> LanguageStrings,
): CharacterDetailStateHolder {
    return CharacterDetailStateHolder(repository, scope, stringsProvider)
}
