package com.luum.michi.app.mediaList.ui.manga.state

import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.incrementedChapters
import com.luum.michi.app.mediaList.domain.manga.model.incrementedVolumes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import com.luum.michi.app.core.domain.model.UserListSort
import com.luum.michi.app.core.domain.model.UserListOrder
import com.luum.michi.app.core.domain.model.MediaSeason
import com.luum.michi.app.core.domain.medialist.MediaListEntryRepository
import com.luum.michi.app.core.domain.medialist.MediaListStatus
import com.luum.michi.app.mediaList.domain.common.MediaListLoader
import com.luum.michi.app.mediaList.domain.common.matchesMediaListFilters
import com.luum.michi.app.mediaList.domain.common.sortMediaListEntries

internal class MangaListStateHolder(
    repository: MangaListRepository,
    private val entryRepository: MediaListEntryRepository,
    private val scope: CoroutineScope,
) {
    private val loader = MediaListLoader(scope) { userId -> repository.loadList(userId) }

    var currentSortOption = UserListSort.FOLLOW_LIST
    var currentSortOrder = UserListOrder.DESCENDING
    var isFilterPersisted = false

    var filterSeason: MediaSeason? = null
    var filterGenres = emptyList<String>()
    var filterFormats = emptyList<String>()
    var filterYear: Int? = null

    val entries: List<MangaListEntry> get() = loader.entries
    val isLoading: Boolean get() = loader.isLoading
    val isRefreshing: Boolean get() = loader.isRefreshing
    val error: NetworkError? get() = loader.error

    fun updateSort(option: UserListSort, order: UserListOrder, persist: Boolean) {
        currentSortOption = option
        currentSortOrder = order
        isFilterPersisted = persist
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
                progress = updatedEntry.chaptersProgress,
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
            .filter { it.status == section }
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
