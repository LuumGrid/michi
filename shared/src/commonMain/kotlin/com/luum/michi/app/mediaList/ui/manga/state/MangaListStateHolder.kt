package com.luum.michi.app.mediaList.ui.manga.state

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.incrementedChapters
import com.luum.michi.app.mediaList.domain.manga.model.incrementedVolumes
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

internal class MangaListStateHolder(
    repository: MangaListRepository,
    private val entryRepository: MediaListEntryRepository,
    private val scope: CoroutineScope,
    private val sortPersistence: SortPersistence? = null,
    private val sortScope: SortScope = SortScope.MANGA,
) {
    private val loader = MediaListLoader(scope) { userId -> repository.loadList(userId) }

    // Snapshot-backed so filter/sort sheets recompose the list on every
    // intent (plain vars would apply silently until the next load).
    var currentSortOption by mutableStateOf(UserListSort.FOLLOW_LIST)
    var currentSortOrder by mutableStateOf(UserListOrder.DESCENDING)
    var isSortPersisted by mutableStateOf(false)

    var filterSeason by mutableStateOf<MediaSeason?>(null)
    var filterGenres by mutableStateOf(emptyList<String>())
    var filterFormats by mutableStateOf(emptyList<String>())
    var filterYear by mutableStateOf<Int?>(null)

    val entries: List<MangaListEntry> get() = loader.entries
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

    fun load(userId: Int, forceRefresh: Boolean = false) = loader.load(userId, forceRefresh)

    fun incrementChapters(entry: MangaListEntry) {
        val index = loader.indexOf { it.id == entry.id }
        if (index == -1) return

        val originalEntry = loader[index]
        val updatedEntry = originalEntry.incrementedChapters()
        loader[index] = updatedEntry

        scope.launch {
            val result = entryRepository.saveProgress(
                mediaId = entry.id,
                progress = updatedEntry.chaptersProgress,
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

    fun incrementVolumes(entry: MangaListEntry) {
        val index = loader.indexOf { it.id == entry.id }
        if (index == -1) return

        val originalEntry = loader[index]
        val updatedEntry = originalEntry.incrementedVolumes()
        loader[index] = updatedEntry

        scope.launch {
            val result = entryRepository.saveProgress(
                mediaId = entry.id,
                // Novels track volumes only: chapters stay omitted so the
                // server leaves them intact instead of resetting to stale 0.
                progress = if (updatedEntry.tracksByVolume) null else updatedEntry.chaptersProgress,
                status = updatedEntry.status.toMediaListStatus(),
                progressVolumes = updatedEntry.volumesProgress,
            )
            if (result is NetworkResult.Failure) {
                val currentIndex = loader.indexOf { it.id == entry.id }
                if (currentIndex != -1) {
                    loader[currentIndex] = originalEntry
                }
            }
        }
    }

    fun entriesInSection(section: MangaListSection): List<MangaListEntry> {
        val filtered = loader.entries
            // ALL has no status of its own: no entry carries it, so filtering
            // by it would always come back empty (counts already treat ALL
            // as the whole list).
            .filter { section == MangaListSection.ALL || it.status == section }
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
            progressOrder = compareBy { it.chaptersProgress },
            // AniList exposes no next-chapter date for manga: degrade to list order.
            airingOrder = compareBy { it.originalIndex },
        )
    }

    fun countInSection(section: MangaListSection): Int =
        if (section == MangaListSection.ALL) loader.entries.size
        else loader.entries.count { it.status == section }
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

private fun MangaListSection.toMediaListStatus(): MediaListStatus = when (this) {
    MangaListSection.ALL -> MediaListStatus.CURRENT
    MangaListSection.CURRENT -> MediaListStatus.CURRENT
    MangaListSection.COMPLETED -> MediaListStatus.COMPLETED
    MangaListSection.PAUSED -> MediaListStatus.PAUSED
    MangaListSection.DROPPED -> MediaListStatus.DROPPED
    MangaListSection.PLANNING -> MediaListStatus.PLANNING
    MangaListSection.REPEATING -> MediaListStatus.REPEATING
}
