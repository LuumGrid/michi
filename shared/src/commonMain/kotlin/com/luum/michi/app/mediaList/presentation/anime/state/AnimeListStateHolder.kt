package com.luum.michi.app.mediaList.presentation.anime.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.incremented
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.anilist.medialist.MediaListEntryRepository
import com.luum.michi.app.core.anilist.medialist.MediaListStatus
import com.luum.michi.app.mediaList.presentation.common.MediaListLoader
import com.luum.michi.app.mediaList.presentation.common.matchesMediaListFilters

internal class AnimeListStateHolder(
    repository: AnimeListRepository,
    private val entryRepository: MediaListEntryRepository,
    private val scope: CoroutineScope,
) {
    private val loader = MediaListLoader(scope) { userId -> repository.loadList(userId) }

    var currentSortOption by mutableStateOf(UserListSort.FOLLOW_LIST)
    var currentSortOrder by mutableStateOf(UserListOrder.DESCENDING)
    var isFilterPersisted by mutableStateOf(false)

    var filterSeason by mutableStateOf<String?>(null)
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
        isFilterPersisted = persist
    }

    /** Client-side filters (the whole list is already loaded): season/genre/format/year. */
    fun updateListFilters(season: String?, genres: List<String>, formats: List<String>, year: Int?) {
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
        val sorted = when (currentSortOption) {
            UserListSort.FOLLOW_LIST -> filtered.sortedBy { it.originalIndex }
            UserListSort.TITLE -> filtered.sortedBy { it.title }
            UserListSort.SCORE -> filtered.sortedBy { it.score }
            UserListSort.PROGRESS -> filtered.sortedBy { it.progress }
            UserListSort.LAST_UPDATED -> filtered.sortedBy { it.updatedAt }
            UserListSort.LAST_ADDED -> filtered.sortedBy { it.id }
            UserListSort.START_DATE -> filtered.sortedBy { it.startedAtInt }
            UserListSort.COMPLETED_DATE -> filtered.sortedBy { it.completedAtInt }
            UserListSort.RELEASE_DATE -> filtered.sortedBy { it.releaseDateInt }
            UserListSort.AVERAGE_SCORE -> filtered.sortedBy { it.averageScore }
            UserListSort.POPULARITY -> filtered.sortedBy { it.popularity }
            UserListSort.FAVORITES -> filtered.sortedBy { it.favouritesCount }
            UserListSort.TRENDING -> filtered.sortedBy { it.trending }
            UserListSort.PRIORITY -> filtered.sortedBy { it.priority }
            UserListSort.NEXT_AIRING -> filtered.sortedBy { it.nextAiringAt }
        }
        return if (currentSortOrder == UserListOrder.DESCENDING) {
            sorted.reversed()
        } else {
            sorted
        }
    }

    fun countInSection(section: AnimeListSection): Int =
        if (section == AnimeListSection.ALL) loader.entries.size
        else loader.entries.count { it.status == section }
}

@Composable
internal fun rememberAnimeListStateHolder(
    repository: AnimeListRepository,
    entryRepository: MediaListEntryRepository,
    viewerId: Int,
): AnimeListStateHolder {
    val scope = rememberCoroutineScope()
    return remember(viewerId) {
        AnimeListStateHolder(repository, entryRepository, scope)
    }
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
