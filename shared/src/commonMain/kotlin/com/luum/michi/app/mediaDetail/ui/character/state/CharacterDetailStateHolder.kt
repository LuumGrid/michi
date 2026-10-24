package com.luum.michi.app.mediaDetail.ui.character.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterDetail
import com.luum.michi.app.mediaDetail.domain.character.model.CharacterMediaItem
import com.luum.michi.app.mediaDetail.ui.state.DetailStateHolderBase
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope

internal data class CharacterDetailSnapshot(
    val detail: CharacterDetail,
    val media: Page<CharacterMediaItem>,
    val voiceLanguage: String,
)

internal class CharacterDetailStateHolder(
    private val repository: CharacterDetailRepository,
    scope: CoroutineScope,
    // Read at request launch: a language change keeps the holder (and its cache).
    private val stringsProvider: () -> LanguageStrings,
) : DetailStateHolderBase<CharacterDetail, CharacterDetailSnapshot>(scope) {

    /** Voice-actor language for the media edges; changing it re-fetches. */
    var voiceLanguage: String by mutableStateOf("JAPANESE")
        private set

    /** Appearances grid (one row per media). */
    val media = PagedList<CharacterMediaItem> { a, b -> (a + b).distinctBy { it.mediaId } }

    override fun isFavouriteOf(detail: CharacterDetail): Boolean = detail.isFavourite

    fun loadMore() {
        val id = currentDetailId ?: return
        media.loadMore(
            scope = scope,
            isCurrent = { currentDetailId == id },
            fetch = { page -> repository.loadMediaPage(id, page, voiceLanguage) },
            onLoaded = { pageLoaded(id) },
            onError = { pageError(it) },
        )
    }

    /** Voice-actor language change: drop the cache and re-fetch with the new cast. */
    fun selectVoiceLanguage(language: String) {
        if (language == voiceLanguage) return
        voiceLanguage = language
        refresh()
    }

    override suspend fun fetchDetail(id: Int): NetworkResult<CharacterDetail> =
        repository.loadDetail(id, voiceLanguage, stringsProvider())

    override fun onDetailLoaded(detail: CharacterDetail) {
        media.set(detail.media)
    }

    override fun cancelPages() {
        media.cancel()
    }

    override fun clearPages() {
        media.clear()
    }

    override fun snapshot(detail: CharacterDetail): CharacterDetailSnapshot = CharacterDetailSnapshot(
        detail = detail,
        media = media.snapshot(),
        voiceLanguage = voiceLanguage,
    )

    override fun restore(snapshot: CharacterDetailSnapshot) {
        detail = snapshot.detail
        media.set(snapshot.media)
    }

    override fun isSnapshotValid(snapshot: CharacterDetailSnapshot): Boolean =
        snapshot.voiceLanguage == voiceLanguage

    override fun withFavourite(detail: CharacterDetail, value: Boolean): CharacterDetail =
        detail.copy(isFavourite = value)

    override fun snapshotWithFavourite(snapshot: CharacterDetailSnapshot, value: Boolean): CharacterDetailSnapshot =
        snapshot.copy(detail = snapshot.detail.copy(isFavourite = value))

    override fun isFavouriteOfSnapshot(snapshot: CharacterDetailSnapshot): Boolean = snapshot.detail.isFavourite

    override suspend fun requestToggleFavourite(id: Int, detail: CharacterDetail): NetworkResult<Unit> =
        repository.toggleFavourite(id)
}
