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
     * Recommendations (per visit; not part of the cache snapshot). Loaded on
     * demand the first time and paged from there.
     */
    val recommendations = PagedList<MediaRecommendationEntry> { a, b -> (a + b).distinctBy { it.id } }

    // Set by [refresh] before the reload: cancelPages() also runs on a new media,
    // where "had recommendations" means nothing, so the flag is only meaningful
    // on the refresh path.
    private var reloadRecommendationsAfterRefresh = false

    /** The media keeps its flag on the visible detail (derived, never a flag). */
    override val isFavourite: Boolean get() = detail?.isFavourite == true

    override fun refresh() {
        // Cancelling the pagination would drop the initial recommendations fetch;
        // remember it (or an already-loaded list) so it is relaunched once the
        // detail answers.
        reloadRecommendationsAfterRefresh =
            recommendations.isLoadingMore || recommendations.items.isNotEmpty()
        super.refresh()
    }

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

    /** Ensures the recommendations first page is loaded (no-op once it has items). */
    fun loadRecommendations() {
        if (recommendations.items.isNotEmpty()) return
        val id = currentDetailId ?: return
        recommendations.loadFirst(
            scope = scope,
            isCurrent = { currentDetailId == id },
            fetch = { repository.loadRecommendations(id) },
            onError = { pageError(it) },
        )
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
    )

    override fun restore(snapshot: MediaDetailSnapshot) {
        detail = snapshot.detail
        characters.set(snapshot.characters)
        staff.set(snapshot.staff)
        // Recommendations are per visit: a cached restore must not keep the
        // previous media's list.
        recommendations.clear()
    }

    override fun withFavourite(detail: MediaDetail, value: Boolean): MediaDetail =
        detail.copy(isFavourite = value)

    override fun snapshotWithFavourite(snapshot: MediaDetailSnapshot, value: Boolean): MediaDetailSnapshot =
        snapshot.copy(detail = snapshot.detail.copy(isFavourite = value))

    override suspend fun requestToggleFavourite(id: Int, detail: MediaDetail): NetworkResult<Unit> =
        entryRepository.toggleFavourite(id, detail.type == MediaDetailType.MANGA)

    override fun onRefreshed(id: Int) {
        if (reloadRecommendationsAfterRefresh) {
            recommendations.clear()
            loadRecommendations()
        }
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
