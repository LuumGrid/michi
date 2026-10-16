package com.luum.michi.app.mediaList.ui.manga.state

import com.luum.michi.app.core.network.domain.NetworkError
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.incrementedChapters
import com.luum.michi.app.mediaList.domain.manga.model.incrementedVolumes
import com.luum.michi.app.mediaList.domain.manga.model.toMediaListStatus
import kotlinx.coroutines.CoroutineScope

import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.mediaList.domain.common.sectionCount
import com.luum.michi.app.mediaList.domain.common.sectionEntries
import com.luum.michi.app.mediaList.ui.common.state.ListProgressSaver
import com.luum.michi.app.mediaList.ui.common.state.MediaListHolder
import com.luum.michi.app.mediaList.ui.common.state.MediaListLoader
import com.luum.michi.app.core.storage.domain.SortPersistence
import com.luum.michi.app.core.storage.domain.SortScope

internal class MangaListStateHolder(
    repository: MangaListRepository,
    entryRepository: MediaListEntryRepository,
    scope: CoroutineScope,
    private val sortPersistence: SortPersistence? = null,
    private val sortScope: SortScope = SortScope.MANGA,
    onEntrySaved: () -> Unit = {},
) : MediaListHolder<MangaListEntry, MangaListSection> {
    private val loader = MediaListLoader(scope) { userId ->
        repository.loadList(userId, lastSplitCompleted)
    }
    private val progressSaver = ListProgressSaver(
        loader = loader,
        scope = scope,
        save = { entry ->
            entryRepository.saveProgress(
                mediaId = entry.id,
                // Novels track volumes only: chapters stay omitted so the
                // server leaves them intact instead of resetting to stale 0.
                progress = if (entry.tracksByVolume) null else entry.chaptersProgress,
                status = entry.status.toMediaListStatus(),
                progressVolumes = entry.volumesProgress,
            )
        },
        onSaved = onEntrySaved,
    )

    // Snapshot-backed so filter/sort sheets recompose the list on every
    // intent (plain vars would apply silently until the next load).
    override var currentSortOption by mutableStateOf(UserListSort.FOLLOW_LIST)
    override var currentSortOrder by mutableStateOf(UserListOrder.DESCENDING)
    var isSortPersisted by mutableStateOf(false)

    override var filterSeason by mutableStateOf<MediaSeason?>(null)
    override var filterGenres by mutableStateOf(emptyList<String>())
    override var filterFormats by mutableStateOf(emptyList<String>())
    override var filterYear by mutableStateOf<Int?>(null)

    override val entries: List<MangaListEntry> get() = loader.entries
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
        // A reload replaces every entry: a stale confirmed value must not survive it.
        progressSaver.onReload()
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

    fun incrementChapters(entry: MangaListEntry) {
        progressSaver.enqueue(entry.id) { it.incrementedChapters(lastSplitCompleted) }
    }

    fun incrementVolumes(entry: MangaListEntry) {
        progressSaver.enqueue(entry.id) { it.incrementedVolumes(lastSplitCompleted) }
    }

    override fun entriesInSection(section: MangaListSection, hideAdult: Boolean): List<MangaListEntry> =
        loader.entries.sectionEntries(
            // ALL has no status of its own: no entry carries it, so filtering
            // by it would always come back empty (counts already treat ALL as
            // the whole list).
            sectionPredicate = { section == MangaListSection.ALL || it.status == section },
            hideAdult = hideAdult,
            filterSeason = filterSeason,
            filterGenres = filterGenres,
            filterYear = filterYear,
            filterFormats = filterFormats,
            sort = currentSortOption,
            order = currentSortOrder,
            progressOrder = compareBy { it.chaptersProgress },
            // AniList exposes no next-chapter date for manga: degrade to list order.
            airingOrder = compareBy { it.originalIndex },
        )

    // One rule: counts reflect the loaded list, never view state (sheet
    // filters, search query, 18+ setting).
    override fun countInSection(section: MangaListSection): Int =
        loader.entries.sectionCount(section == MangaListSection.ALL) { it.status == section }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createMangaListStateHolder(
    repository: MangaListRepository,
    entryRepository: MediaListEntryRepository,
    scope: CoroutineScope,
    viewerId: Int,
): MangaListStateHolder {
    return MangaListStateHolder(repository, entryRepository, scope)
}
