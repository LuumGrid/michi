package com.luum.michi.app.mediaList.ui.common.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.media.domain.MediaSeason
import com.luum.michi.app.core.medialist.domain.SortPersistence
import com.luum.michi.app.core.medialist.domain.SortScope
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaList.domain.common.SortableMediaListEntry
import com.luum.michi.app.mediaList.domain.common.sectionCount
import com.luum.michi.app.mediaList.domain.common.sectionEntries
import kotlinx.coroutines.CoroutineScope

/**
 * Shared state/lifecycle of the two user-list holders (anime, manga): the
 * TTL loader, the optimistic `+1` saver, sort/filter state, the section
 * filtering/counting and `load`. A subclass only wires its own network calls
 * and its own `+1` increments.
 *
 * [T] is the list entry, [S] the section enum. The hooks are the minimum a list
 * must supply; nothing abstract runs in `init` (the hooks live inside the loader
 * and saver lambdas, invoked only when `load`/an increment is called).
 */
internal abstract class MediaListStateHolderBase<T : SortableMediaListEntry, S>(
    scope: CoroutineScope,
    private val sortPersistence: SortPersistence? = null,
    private val sortScope: SortScope,
    onEntrySaved: () -> Unit = {},
) : MediaListHolder<T, S> {
    private val loader = MediaListLoader(
        scope = scope,
        fetch = { userId -> fetchList(userId, lastSplitCompleted) },
        hydrate = { userId -> loadCachedList(userId, lastSplitCompleted) },
    )
    private val progressSaver = ListProgressSaver(
        loader = loader,
        scope = scope,
        save = { entry -> saveProgress(entry) },
        onSaved = onEntrySaved,
    )

    // A reload replaces every entry: a stale confirmed value (or a `+1` error)
    // must not survive it. Wired to a real replacement only, so a tab switch
    // that loads nothing (TTL/join) never wipes the in-flight rollback.
    init {
        loader.onReplaced = { progressSaver.onReload() }
    }

    // Snapshot-backed so filter/sort sheets recompose the list on every
    // intent (plain vars would apply silently until the next load).
    override var currentSortOption by mutableStateOf(UserListSort.FOLLOW_LIST)
    override var currentSortOrder by mutableStateOf(UserListOrder.DESCENDING)

    override var filterSeason by mutableStateOf<MediaSeason?>(null)
    override var filterGenres by mutableStateOf(emptyList<String>())
    override var filterFormats by mutableStateOf(emptyList<String>())
    override var filterYear by mutableStateOf<Int?>(null)

    override val entries: List<T> get() = loader.entries
    override val isLoading: Boolean get() = loader.isLoading
    val isRefreshing: Boolean get() = loader.isRefreshing
    override val error: NetworkError? get() = loader.error

    /** True only once the network confirmed the entries (see [MediaListLoader]). */
    override val canEditEntries: Boolean get() = loader.isConfirmed

    /** Non-blocking failure of a `+1` save, shown as a compact dismissible row. */
    override val actionError: NetworkError? get() = progressSaver.actionError

    /** Split flag of the last mapping (entries always match it). Plain var:
     * only `load` writes it; the subclasses read it to remap on a `+1`. */
    protected var lastSplitCompleted = true

    /** Dismisses the compact action-error row. */
    override fun clearActionError() {
        progressSaver.clearActionError()
    }

    override fun updateSort(option: UserListSort, order: UserListOrder, persist: Boolean) {
        currentSortOption = option
        currentSortOrder = order
        if (persist) {
            sortPersistence?.saveSort(sortScope, option.name, order.name)
        }
    }

    /** Client-side filters (the whole list is already loaded): season/genre/format/year. */
    override fun updateListFilters(season: MediaSeason?, genres: List<String>, formats: List<String>, year: Int?) {
        filterSeason = season
        filterGenres = genres
        filterFormats = formats
        filterYear = year
    }

    fun load(userId: Int, forceRefresh: Boolean = false, splitCompleted: Boolean = true) {
        // Split flips re-map every entry: bypass the TTL so the toggle
        // applies immediately instead of on the next cold load. The fetch
        // reads lastSplitCompleted at execution, so entries always match it.
        val force = forceRefresh || splitCompleted != lastSplitCompleted
        lastSplitCompleted = splitCompleted
        loader.load(userId, force)
    }

    /** Applies an optimistic `+1` (the subclasses' increment ops call this). */
    protected fun enqueue(id: Int, transform: (T) -> T) {
        // Hydrated-only entries are not confirmed by the network: a `+1` would
        // send progress calculated from stale data. Wait for the revalidation.
        if (!canEditEntries) return
        progressSaver.enqueue(id, transform)
    }

    override fun entriesInSection(section: S, hideAdult: Boolean): List<T> =
        loader.entries.sectionEntries(
            // ALL has no status of its own: no entry carries it, so filtering
            // by it would always come back empty (counts already treat ALL as
            // the whole list).
            sectionPredicate = { section == allSection || statusOf(it) == section },
            hideAdult = hideAdult,
            filterSeason = filterSeason,
            filterGenres = filterGenres,
            filterYear = filterYear,
            filterFormats = filterFormats,
            sort = currentSortOption,
            order = currentSortOrder,
            progressOrder = progressOrder,
            airingOrder = airingOrder,
        )

    // One rule: counts reflect the loaded list, never view state (sheet
    // filters, search query, 18+ setting).
    override fun countInSection(section: S): Int =
        loader.entries.sectionCount(section == allSection) { statusOf(it) == section }

    /** The section that carries the whole list (counts treat it as everything). */
    protected abstract val allSection: S

    /** The section an entry belongs to (split decides the per-format COMPLETED). */
    protected abstract fun statusOf(entry: T): S

    /** Comparator for [UserListSort.PROGRESS] (episodes vs chapters). */
    protected abstract val progressOrder: Comparator<T>

    /** Comparator for [UserListSort.NEXT_AIRING] (airing date vs list order). */
    protected abstract val airingOrder: Comparator<T>

    /** Fetches the whole list (both lists are full `MediaListCollection`s). */
    protected abstract suspend fun fetchList(userId: Int, splitCompleted: Boolean): NetworkResult<List<T>>

    /** The persisted list already mapped for [splitCompleted], or null. */
    protected abstract suspend fun loadCachedList(userId: Int, splitCompleted: Boolean): List<T>?

    /** Sends one entry's progress (each list maps its own shape). */
    protected abstract suspend fun saveProgress(entry: T): NetworkResult<Unit>
}
