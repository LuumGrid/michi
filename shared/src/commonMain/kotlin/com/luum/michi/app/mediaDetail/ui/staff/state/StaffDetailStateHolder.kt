package com.luum.michi.app.mediaDetail.ui.staff.state

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffCharacterItem
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffDetail
import com.luum.michi.app.mediaDetail.domain.staff.model.StaffMediaItem
import com.luum.michi.app.mediaDetail.ui.state.DetailStateHolderBase
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope

/**
 * Cache snapshot holds BOTH paginated lists independently so that a page
 * loaded on media-only doesn't evict the already-loaded character list and
 * vice versa.
 */
internal data class StaffDetailSnapshot(
    val detail: StaffDetail,
    val media: Page<StaffMediaItem>,
    val characters: Page<StaffCharacterItem>,
)

internal class StaffDetailStateHolder(
    private val repository: StaffDetailRepository,
    scope: CoroutineScope,
    // Read at request launch: a language change keeps the holder (and its cache).
    private val stringsProvider: () -> LanguageStrings,
) : DetailStateHolderBase<StaffDetail, StaffDetailSnapshot>(scope) {

    // ── media (roles) list ──
    // The same work can repeat with several roles (Director + Storyboard), so
    // rows are deduped by (work, role), not by work alone.
    val media = PagedList<StaffMediaItem> { a, b -> (a + b).distinctBy { it.mediaId to it.staffRole } }

    // ── character list ──
    // The same character in two works is two rows: dedupe by (character, work).
    val characters = PagedList<StaffCharacterItem> { a, b -> (a + b).distinctBy { it.characterId to it.mediaId } }

    /** Derived from the visible detail: no separate flag can drift from it. */
    override val isFavourite: Boolean get() = detail?.isFavourite == true

    /** Loads the next page of the MEDIA (roles) list only. */
    fun loadMoreMedia() {
        val id = currentDetailId ?: return
        media.loadMore(
            scope = scope,
            isCurrent = { currentDetailId == id },
            fetch = { page -> repository.loadMediaPage(id, page) },
            onLoaded = { pageLoaded(id) },
            onError = { pageError(it) },
        )
    }

    /** Loads the next page of the CHARACTERS list only. */
    fun loadMoreCharacters() {
        val id = currentDetailId ?: return
        characters.loadMore(
            scope = scope,
            isCurrent = { currentDetailId == id },
            fetch = { page -> repository.loadCharacterPage(id, page) },
            onLoaded = { pageLoaded(id) },
            onError = { pageError(it) },
        )
    }

    override suspend fun fetchDetail(id: Int): NetworkResult<StaffDetail> =
        repository.loadDetail(id, stringsProvider())

    override fun onDetailLoaded(detail: StaffDetail) {
        media.set(detail.media)
        characters.set(detail.characters)
    }

    override fun cancelPages() {
        media.cancel()
        characters.cancel()
    }

    override fun clearPages() {
        media.clear()
        characters.clear()
    }

    override fun snapshot(detail: StaffDetail): StaffDetailSnapshot = StaffDetailSnapshot(
        detail = detail,
        media = media.snapshot(),
        characters = characters.snapshot(),
    )

    override fun restore(snapshot: StaffDetailSnapshot) {
        detail = snapshot.detail
        media.set(snapshot.media)
        characters.set(snapshot.characters)
    }

    override fun withFavourite(detail: StaffDetail, value: Boolean): StaffDetail =
        detail.copy(isFavourite = value)

    override fun snapshotWithFavourite(snapshot: StaffDetailSnapshot, value: Boolean): StaffDetailSnapshot =
        snapshot.copy(detail = snapshot.detail.copy(isFavourite = value))

    override suspend fun requestToggleFavourite(id: Int, detail: StaffDetail): NetworkResult<Unit> =
        repository.toggleFavourite(id)
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createStaffDetailStateHolder(
    repository: StaffDetailRepository,
    scope: CoroutineScope,
    stringsProvider: () -> LanguageStrings,
): StaffDetailStateHolder {
    return StaffDetailStateHolder(repository, scope, stringsProvider)
}
