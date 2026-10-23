package com.luum.michi.app.mediaDetail.ui.media.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.AnimeThemesRepository
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.model.AnimeTheme
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
import com.luum.michi.app.mediaDetail.ui.state.DetailStateHolderBase
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Snapshot of MediaDetail content that can be restored without a network call. */
internal data class MediaDetailSnapshot(
    val detail: MediaDetail,
    val characters: Page<MediaCharacterEntry>,
    val staff: Page<MediaStaffEntry>,
    val recommendations: Page<MediaRecommendationEntry>,
)

internal class MediaDetailStateHolder(
    private val repository: MediaDetailRepository,
    private val entryRepository: MediaListEntryRepository,
    private val themesRepository: AnimeThemesRepository,
    scope: CoroutineScope,
    // Read at request launch (not stored): a language change must not recreate
    // the holder and drop its cache, but a new request must use the new strings.
    private val stringsProvider: () -> LanguageStrings,
) : DetailStateHolderBase<MediaDetail, MediaDetailSnapshot>(scope) {

    /** Characters grid; a character is one row per media (dedup by id). */
    val characters = PagedList<MediaCharacterEntry> { a, b -> (a + b).distinctBy { it.characterId } }

    /** Staff grid; a person can repeat with a different role (dedup by id + role). */
    val staff = PagedList<MediaStaffEntry> { a, b -> (a + b).distinctBy { it.staffId to it.role } }

    /**
     * Recommendations grid; seeded from the media detail's embedded first page
     * and paged from page 2 on. Pages 2+ cost a request; reopening a media
     * whose snapshot is cached restores this list with no network call.
     */
    val recommendations = PagedList<MediaRecommendationEntry> { a, b -> (a + b).distinctBy { it.id } }

    /**
     * AnimeThemes songs of the current anime: null until loaded (or while
     * another anime loads); empty when there are none or the request failed
     * (the section is an extra and is simply not painted).
     */
    var themes: List<AnimeTheme>? by mutableStateOf(null)
        private set

    private var themesFor: Int? = null
    private var themesJob: Job? = null

    override fun isFavouriteOf(detail: MediaDetail): Boolean = detail.isFavourite

    fun loadMoreCharacters() {
        val id = currentDetailId ?: return
        characters.loadMore(
            scope = scope,
            isCurrent = { currentDetailId == id },
            fetch = { page -> repository.loadCharactersPage(id, page = page) },
            onLoaded = { pageLoaded(id) },
            onError = { pageError(it) },
        )
    }

    fun loadMoreStaff() {
        val id = currentDetailId ?: return
        staff.loadMore(
            scope = scope,
            isCurrent = { currentDetailId == id },
            fetch = { page -> repository.loadStaffPage(id, page = page) },
            onLoaded = { pageLoaded(id) },
            onError = { pageError(it) },
        )
    }

    /**
     * The editor saved or deleted the viewer's entry for [mediaId] (from any
     * surface): drop the cached snapshot so a later visit never restores the
     * old entry, and re-fetch when this holder still shows that media so the
     * entry card is not stale.
     */
    fun onEntryChanged(mediaId: Int) {
        dropCached(mediaId)
        if (currentDetailId == mediaId && detail != null) refresh()
    }

    fun loadMoreRecommendations() {
        val id = currentDetailId ?: return
        recommendations.loadMore(
            scope = scope,
            isCurrent = { currentDetailId == id },
            fetch = { page -> repository.loadRecommendations(id, page) },
            onError = { pageError(it) },
        )
    }

    override suspend fun fetchDetail(id: Int): NetworkResult<MediaDetail> {
        // In parallel with AniList: the type is unknown until it answers, so a
        // manga asks once too (AnimeThemes answers empty) and onDetailLoaded drops it.
        loadThemes()
        return repository.loadDetail(id, stringsProvider())
    }

    /**
     * Fetches the themes once per media id, in parallel with the AniList detail
     * (fresh) or as soon as a snapshot is restored. Repeated calls for the same
     * id are no-ops.
     */
    private fun loadThemes() {
        val id = currentDetailId ?: return
        if (themesFor == id) return
        themesFor = id
        themes = null
        themesJob?.cancel()
        themesJob = scope.launch {
            val result = themesRepository.themes(id)
            if (currentDetailId != id || themesFor != id) return@launch
            themes = (result as? NetworkResult.Success<List<AnimeTheme>>)?.value ?: emptyList()
        }
    }

    private fun resetThemesUnless(id: Int) {
        if (themesFor != id) {
            themesJob?.cancel()
            themesJob = null
            themesFor = null
            themes = null
        }
    }

    /** A manga never shows themes: cancel the speculative request and keep
     *  [themesFor] so a refresh of the same manga does not ask again. */
    private fun dropThemes() {
        themesJob?.cancel()
        themesJob = null
        themes = null
    }

    override fun onDetailLoaded(detail: MediaDetail) {
        resetThemesUnless(detail.id)
        characters.set(detail.characters)
        staff.set(detail.staff)
        recommendations.set(detail.recommendations)
        if (detail.type == MediaDetailType.ANIME) loadThemes() else dropThemes()
    }

    override fun cancelPages() {
        characters.cancel()
        staff.cancel()
        recommendations.cancel()
    }

    override fun clearPages() {
        characters.clear()
        staff.clear()
        recommendations.clear()
        themesJob?.cancel()
        themesFor = null
        themes = null
    }

    override fun snapshot(detail: MediaDetail): MediaDetailSnapshot = MediaDetailSnapshot(
        detail = detail,
        characters = characters.snapshot(),
        staff = staff.snapshot(),
        recommendations = recommendations.snapshot(),
    )

    override fun restore(snapshot: MediaDetailSnapshot) {
        resetThemesUnless(snapshot.detail.id)
        detail = snapshot.detail
        characters.set(snapshot.characters)
        staff.set(snapshot.staff)
        recommendations.set(snapshot.recommendations)
        if (snapshot.detail.type == MediaDetailType.ANIME) loadThemes()
    }

    override fun withFavourite(detail: MediaDetail, value: Boolean): MediaDetail =
        detail.copy(isFavourite = value)

    override fun snapshotWithFavourite(snapshot: MediaDetailSnapshot, value: Boolean): MediaDetailSnapshot =
        snapshot.copy(detail = snapshot.detail.copy(isFavourite = value))

    override fun isFavouriteOfSnapshot(snapshot: MediaDetailSnapshot): Boolean = snapshot.detail.isFavourite

    override suspend fun requestToggleFavourite(id: Int, detail: MediaDetail): NetworkResult<Unit> =
        entryRepository.toggleFavourite(id, detail.type == MediaDetailType.MANGA)
}
