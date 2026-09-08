package com.luum.michi.app.mediaList.ui.anime.state

import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.incremented
import com.luum.michi.app.core.domain.network.NetworkError
import com.luum.michi.app.core.domain.network.NetworkResult
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

internal class AnimeListStateHolder(
    repository: AnimeListRepository,
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

    val entries: List<AnimeListEntry> get() = loader.entries
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

    fun entriesInSection(section: AnimeListSection): List<AnimeListEntry> {
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
            progressOrder = compareBy { it.progress },
            airingOrder = compareBy { it.nextAiringAt },
        )
    }

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
