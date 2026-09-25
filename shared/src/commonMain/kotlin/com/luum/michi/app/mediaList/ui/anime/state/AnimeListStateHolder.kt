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

        val originalEntry = loader[index]
        val updatedEntry = originalEntry.incremented()
        loader[index] = updatedEntry

        scope.launch {
            val result = entryRepository.saveProgress(
                mediaId = entry.id,
                progress = updatedEntry.progress,
                status = updatedEntry.status.toMediaListStatus(),
            )
            if (result is NetworkResult.Failure) {
                val currentIndex = loader.indexOf { it.id == entry.id }
                if (currentIndex != -1) {
                    loader[currentIndex] = originalEntry
                }
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
