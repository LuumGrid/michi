package com.luum.michi.app.mediaList.ui.anime.state

import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.incremented
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.mediaList.domain.common.MediaListLoader
import com.luum.michi.app.mediaList.domain.common.matchesMediaListFilters
import com.luum.michi.app.mediaList.domain.common.sortMediaListEntries
import com.luum.michi.app.core.storage.domain.SortPersistence
import com.luum.michi.app.core.storage.domain.SortScope

internal class AnimeListStateHolder(
    repository: AnimeListRepository,
    private val entryRepository: MediaListEntryRepository,
    private val scope: CoroutineScope,
    private val sortPersistence: SortPersistence? = null,
    private val sortScope: SortScope = SortScope.ANIME,
    // Fired after a successful progress save, so Root can invalidate surfaces
    // that show the viewer's status (the calendar).
    private val onEntrySaved: () -> Unit = {},
) {
    private val loader = MediaListLoader(scope) { userId ->
        repository.loadList(userId, lastSplitCompleted)
    }

    // Snapshot-backed so filter/sort sheets recompose the list on every
    // intent (plain vars would apply silently until the next load).
    var currentSortOption by mutableStateOf(UserListSort.FOLLOW_LIST)
    var currentSortOrder by mutableStateOf(UserListOrder.DESCENDING)
    var isSortPersisted by mutableStateOf(false)

    var filterSeason by mutableStateOf<MediaSeason?>(null)
    var filterGenres by mutableStateOf(emptyList<String>())
    var filterFormats by mutableStateOf(emptyList<String>())
    var filterYear by mutableStateOf<Int?>(null)

    val entries: List<AnimeListEntry> get() = loader.entries
    val isLoading: Boolean get() = loader.isLoading
    val isRefreshing: Boolean get() = loader.isRefreshing
    val error: NetworkError? get() = loader.error

    /** Non-blocking failure of a `+1` save, shown as a compact dismissible row. */
    var actionError: NetworkError? by mutableStateOf(null)
        private set

    // One lock per media: saves for the same entry never interleave, and the
    // second `+1` sends the latest progress once the first save finishes.
    private val saveMutexes = mutableMapOf<Int, Mutex>()
    // Last server-confirmed entry per media: the rollback target, never an
    // optimistic value that was never saved. Lives only while a save is pending.
    private val confirmed = mutableMapOf<Int, AnimeListEntry>()

    private fun saveMutexFor(id: Int): Mutex = saveMutexes.getOrPut(id) { Mutex() }

    /** Dismisses the compact action-error row. */
    fun clearActionError() {
        actionError = null
    }

    fun updateSort(option: UserListSort, order: UserListOrder, persist: Boolean) {
        currentSortOption = option
        currentSortOrder = order
        isSortPersisted = persist
        if (persist) {
            sortPersistence?.saveSort(sortScope, option.name, order.name)
        }
    }

    /** Client-side filters (the whole list is already loaded): season/genre/format/year. */
    fun updateListFilters(season: MediaSeason?, genres: List<String>, formats: List<String>, year: Int?) {
        filterSeason = season
        filterGenres = genres
        filterFormats = formats
        filterYear = year
    }

    fun load(userId: Int, forceRefresh: Boolean = false, splitCompleted: Boolean = true) {
        actionError = null
        // A reload replaces every entry: a stale confirmed value must not survive it.
        confirmed.clear()
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
        val index = loader.indexOf { it.id == entry.id }
        if (index == -1) return

        val id = entry.id
        val current = loader[index]
        // Seed the confirmed value before the first tap.
        confirmed.getOrPut(id) { current }
        loader[index] = current.incremented(lastSplitCompleted)

        scope.launch {
            val mutex = saveMutexFor(id)
            mutex.withLock {
                val currentIndex = loader.indexOf { it.id == id }
                if (currentIndex == -1) return@withLock
                // Send the latest state (a queued +1 may have advanced it).
                val sent = loader[currentIndex]
                // The CURRENT confirmed value (a previous save may have advanced
                // it), never a copy captured at tap time. Without one, the
                // reloaded server data is the base.
                val base = confirmed[id] ?: sent
                // Nothing new since the last confirmed value: skip and leave
                // actionError alone.
                if (sent == base) return@withLock
                when (
                    val result = entryRepository.saveProgress(
                        mediaId = id,
                        progress = sent.progress,
                        status = sent.status.toMediaListStatus(),
                    )
                ) {
                    is NetworkResult.Success -> {
                        confirmed[id] = sent
                        actionError = null
                        onEntrySaved()
                    }
                    is NetworkResult.Failure -> {
                        // Revert to the confirmed value, only if unchanged since.
                        val idx = loader.indexOf { it.id == id }
                        if (idx != -1 && loader[idx] == sent) loader[idx] = base
                        actionError = result.error
                    }
                }
            }
            // `confirmed` only lives while saves are pending.
            if (!mutex.isLocked) {
                saveMutexes.remove(id)
                confirmed.remove(id)
            }
        }
    }

    fun entriesInSection(section: AnimeListSection, hideAdult: Boolean = false): List<AnimeListEntry> {
        val filtered = loader.entries
            // ALL has no status of its own: no entry carries it, so filtering
            // by it would always come back empty (counts already treat ALL
            // as the whole list).
            .filter { section == AnimeListSection.ALL || it.status == section }
            // Global 18+ setting (not a sheet filter): applied here so it
            // stays fresh per composition with no holder state and no
            // re-fetch — and testable at the seam like everything else.
            .filter { !hideAdult || !it.isAdult }
            .filter {
                matchesMediaListFilters(
                    season = it.season,
                    genres = it.genres,
                    seasonYear = it.seasonYear,
                    formatName = it.format.name,
                    filterSeason = filterSeason,
                    filterGenres = filterGenres,
                    filterYear = filterYear,
                    filterFormats = filterFormats,
                )
            }
        return sortMediaListEntries(
            entries = filtered,
            sort = currentSortOption,
            order = currentSortOrder,
            progressOrder = compareBy { it.progress },
            airingOrder = compareBy { it.nextAiringAt },
        )
    }

    // One rule: counts reflect the loaded list, never view state (sheet
    // filters, search query, 18+ setting). A filtered-out-but-counted
    // section tells the user something is hiding entries, not that the
    // list emptied — same convention as AniList.
    fun countInSection(section: AnimeListSection): Int =
        if (section == AnimeListSection.ALL) loader.entries.size
        else loader.entries.count { it.status == section }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createAnimeListStateHolder(
    repository: AnimeListRepository,
    entryRepository: MediaListEntryRepository,
    scope: CoroutineScope,
    viewerId: Int,
): AnimeListStateHolder {
    return AnimeListStateHolder(repository, entryRepository, scope)
}

private fun AnimeListSection.toMediaListStatus(): MediaListStatus = when (this) {
    AnimeListSection.ALL -> MediaListStatus.CURRENT
    AnimeListSection.WATCHING -> MediaListStatus.CURRENT
    AnimeListSection.COMPLETED,
    AnimeListSection.COMPLETED_TV,
    AnimeListSection.COMPLETED_MOVIE,
    AnimeListSection.COMPLETED_OVA,
    AnimeListSection.COMPLETED_ONA,
    AnimeListSection.COMPLETED_TV_SHORT,
    AnimeListSection.COMPLETED_SPECIAL -> MediaListStatus.COMPLETED
    AnimeListSection.PAUSED -> MediaListStatus.PAUSED
    AnimeListSection.DROPPED -> MediaListStatus.DROPPED
    AnimeListSection.PLANNING -> MediaListStatus.PLANNING
    AnimeListSection.REWATCHING -> MediaListStatus.REPEATING
}
