package com.luum.michi.app.mediaList.ui.anime.state

import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.incremented
import com.luum.michi.app.mediaList.domain.anime.model.toMediaListStatus
import com.luum.michi.app.core.network.domain.NetworkError
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope

import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.media.domain.MediaSeason
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.mediaList.domain.common.sectionCount
import com.luum.michi.app.mediaList.domain.common.sectionEntries
import com.luum.michi.app.mediaList.ui.common.state.ListProgressSaver
import com.luum.michi.app.mediaList.ui.common.state.MediaListHolder
import com.luum.michi.app.mediaList.ui.common.state.MediaListLoader
import com.luum.michi.app.core.medialist.domain.SortPersistence
import com.luum.michi.app.core.medialist.domain.SortScope

internal class AnimeListStateHolder(
    repository: AnimeListRepository,
    entryRepository: MediaListEntryRepository,
    scope: CoroutineScope,
    private val sortPersistence: SortPersistence? = null,
    private val sortScope: SortScope = SortScope.ANIME,
    onEntrySaved: () -> Unit = {},
) : MediaListHolder<AnimeListEntry, AnimeListSection> {
    private val loader = MediaListLoader(scope) { userId ->
        repository.loadList(userId, lastSplitCompleted)
    }
    private val progressSaver = ListProgressSaver(
        loader = loader,
        scope = scope,
        save = { entry ->
            entryRepository.saveProgress(
                mediaId = entry.id,
                progress = entry.progress,
                status = entry.status.toMediaListStatus(),
            )
        },
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
    var isSortPersisted by mutableStateOf(false)

    override var filterSeason by mutableStateOf<MediaSeason?>(null)
    override var filterGenres by mutableStateOf(emptyList<String>())
    override var filterFormats by mutableStateOf(emptyList<String>())
    override var filterYear by mutableStateOf<Int?>(null)

    override val entries: List<AnimeListEntry> get() = loader.entries
    override val isLoading: Boolean get() = loader.isLoading
    val isRefreshing: Boolean get() = loader.isRefreshing
    override val error: NetworkError? get() = loader.error

    /** Non-blocking failure of a `+1` save, shown as a compact dismissible row. */
    override val actionError: NetworkError? get() = progressSaver.actionError

    /** Dismisses the compact action-error row. */
    override fun clearActionError() {
        progressSaver.clearActionError()
    }

    override fun updateSort(option: UserListSort, order: UserListOrder, persist: Boolean) {
        currentSortOption = option
        currentSortOrder = order
        isSortPersisted = persist
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

    /** Split flag of the last mapping (entries always match it). Plain var:
     * only load() writes it; the UI recomposes off entries + screen params. */
    private var lastSplitCompleted = true

    fun incrementProgress(entry: AnimeListEntry) {
        progressSaver.enqueue(entry.id) { it.incremented(lastSplitCompleted) }
    }

    override fun entriesInSection(section: AnimeListSection, hideAdult: Boolean): List<AnimeListEntry> =
        loader.entries.sectionEntries(
            // ALL has no status of its own: no entry carries it, so filtering
            // by it would always come back empty (counts already treat ALL as
            // the whole list).
            sectionPredicate = { section == AnimeListSection.ALL || it.status == section },
            hideAdult = hideAdult,
            filterSeason = filterSeason,
            filterGenres = filterGenres,
            filterYear = filterYear,
            filterFormats = filterFormats,
            sort = currentSortOption,
            order = currentSortOrder,
            progressOrder = compareBy { it.progress },
            airingOrder = compareBy { it.nextAiringAt },
        )

    // One rule: counts reflect the loaded list, never view state (sheet
    // filters, search query, 18+ setting).
    override fun countInSection(section: AnimeListSection): Int =
        loader.entries.sectionCount(section == AnimeListSection.ALL) { it.status == section }
}
