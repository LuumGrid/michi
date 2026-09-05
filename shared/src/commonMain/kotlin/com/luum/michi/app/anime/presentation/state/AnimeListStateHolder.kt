package com.luum.michi.app.anime.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.anime.domain.AnimeListRepository
import com.luum.michi.app.anime.domain.model.AnimeListEntry
import com.luum.michi.app.anime.domain.model.AnimeListSection
import com.luum.michi.app.anime.domain.model.incremented
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import com.luum.michi.app.core.platform.model.UserListSort
import com.luum.michi.app.core.platform.model.UserListOrder
import com.luum.michi.app.core.medialist.MediaListEntryRepository
import com.luum.michi.app.core.medialist.MediaListStatus
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

internal class AnimeListStateHolder(
    private val repository: AnimeListRepository,
    private val entryRepository: MediaListEntryRepository,
    private val scope: CoroutineScope,
) {
    private val backing = mutableStateListOf<AnimeListEntry>()
    private var loadingState by mutableStateOf(false)
    private var refreshingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null
    private var lastUserId: Int? = null

    var currentSortOption by mutableStateOf(UserListSort.FOLLOW_LIST)
    var currentSortOrder by mutableStateOf(UserListOrder.DESCENDING)
    var isFilterPersisted by mutableStateOf(false)

    val entries: List<AnimeListEntry> get() = backing
    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState
    val error: NetworkError? get() = errorState

    fun updateSort(option: UserListSort, order: UserListOrder, persist: Boolean) {
        currentSortOption = option
        currentSortOrder = order
        isFilterPersisted = persist
    }

    fun load(userId: Int, forceRefresh: Boolean = false) {
        val mark = lastLoaded
        if (!forceRefresh && lastUserId == userId && mark != null
            && mark.elapsedNow() < CACHE_TTL && backing.isNotEmpty()
        ) return
        val isRefresh = forceRefresh && backing.isNotEmpty()
        scope.launch {
            if (isRefresh) refreshingState = true else loadingState = true
            errorState = null
            try {
                when (val result = repository.loadList(userId)) {
                    is NetworkResult.Success -> {
                        backing.clear()
                        backing.addAll(result.value)
                        lastLoaded = timeMark.markNow()
                        lastUserId = userId
                    }
                    is NetworkResult.Failure -> {
                        errorState = result.error
                    }
                }
            } finally {
                loadingState = false
                refreshingState = false
            }
        }
    }

    companion object {
        private val CACHE_TTL = 5.minutes
    }

    fun incrementProgress(entry: AnimeListEntry) {
        val index = backing.indexOfFirst { it.id == entry.id }
        if (index == -1) return

        val originalEntry = backing[index]
        val updatedEntry = originalEntry.incremented()
        backing[index] = updatedEntry

        scope.launch {
            val result = entryRepository.saveProgress(
                mediaId = entry.id,
                progress = updatedEntry.progress,
                status = updatedEntry.status.toMediaListStatus(),
            )
            if (result is NetworkResult.Failure) {
                val currentIndex = backing.indexOfFirst { it.id == entry.id }
                if (currentIndex != -1) {
                    backing[currentIndex] = originalEntry
                }
            }
        }
    }

    fun entriesInSection(section: AnimeListSection): List<AnimeListEntry> {
        val filtered = backing.filter { it.status == section }
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
        if (section == AnimeListSection.ALL) backing.size
        else backing.count { it.status == section }
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
