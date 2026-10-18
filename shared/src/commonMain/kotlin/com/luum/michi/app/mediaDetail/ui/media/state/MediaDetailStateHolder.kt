package com.luum.michi.app.mediaDetail.ui.media.state

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
import com.luum.michi.app.mediaDetail.ui.state.DetailStateHolderBase
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope

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

    /**
     * Applies a favourite change made from the editor to this media's state and
     * cached snapshot, so closing the editor never leaves the detail stale.
     */
    fun applyFavouriteChange(mediaId: Int, value: Boolean) {
        updateCachedFavourite(mediaId, value)
        if (currentDetailId == mediaId) detail = detail?.let { withFavourite(it, value) }
    }

    override suspend fun fetchDetail(id: Int): NetworkResult<MediaDetail> =
        repository.loadDetail(id, stringsProvider())

    override fun onDetailLoaded(detail: MediaDetail) {
        characters.set(detail.characters)
        staff.set(detail.staff)
        recommendations.set(detail.recommendations)
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
    }

    override fun snapshot(detail: MediaDetail): MediaDetailSnapshot = MediaDetailSnapshot(
        detail = detail,
        characters = characters.snapshot(),
        staff = staff.snapshot(),
        recommendations = recommendations.snapshot(),
    )

    override fun restore(snapshot: MediaDetailSnapshot) {
        detail = snapshot.detail
        characters.set(snapshot.characters)
        staff.set(snapshot.staff)
        recommendations.set(snapshot.recommendations)
    }

    override fun withFavourite(detail: MediaDetail, value: Boolean): MediaDetail =
        detail.copy(isFavourite = value)

    override fun snapshotWithFavourite(snapshot: MediaDetailSnapshot, value: Boolean): MediaDetailSnapshot =
        snapshot.copy(detail = snapshot.detail.copy(isFavourite = value))

    override suspend fun requestToggleFavourite(id: Int, detail: MediaDetail): NetworkResult<Unit> =
        entryRepository.toggleFavourite(id, detail.type == MediaDetailType.MANGA)
}
