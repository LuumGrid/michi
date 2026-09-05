package com.luum.michi.app.manga.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.manga.domain.MangaListRepository
import com.luum.michi.app.manga.domain.model.MangaListEntry
import com.luum.michi.app.manga.domain.model.MangaListSection
import com.luum.michi.app.manga.domain.model.incrementedChapters
import com.luum.michi.app.manga.domain.model.incrementedVolumes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import com.luum.michi.app.core.platform.model.UserListSort
import com.luum.michi.app.core.platform.model.UserListOrder
import com.luum.michi.app.core.medialist.MediaListEntryRepository
import com.luum.michi.app.core.medialist.MediaListStatus
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

internal class MangaListStateHolder(
    private val repository: MangaListRepository,
    private val entryRepository: MediaListEntryRepository,
    private val scope: CoroutineScope,
) {
    private val backing = mutableStateListOf<MangaListEntry>()
    private var loadingState by mutableStateOf(false)
    private var refreshingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null
    private var lastUserId: Int? = null

    var currentSortOption by mutableStateOf(UserListSort.FOLLOW_LIST)
    var currentSortOrder by mutableStateOf(UserListOrder.DESCENDING)
    var isFilterPersisted by mutableStateOf(false)

    val entries: List<MangaListEntry> get() = backing
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

    fun incrementChapters(entry: MangaListEntry) {
        val index = backing.indexOfFirst { it.id == entry.id }
        if (index == -1) return

        val originalEntry = backing[index]
        val updatedEntry = originalEntry.incrementedChapters()
        backing[index] = updatedEntry

        scope.launch {
            val result = entryRepository.saveProgress(
                mediaId = entry.id,
                progress = updatedEntry.chaptersProgress,
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

    fun incrementVolumes(entry: MangaListEntry) {
        val index = backing.indexOfFirst { it.id == entry.id }
        if (index == -1) return

        val originalEntry = backing[index]
        val updatedEntry = originalEntry.incrementedVolumes()
        backing[index] = updatedEntry

        scope.launch {
            val result = entryRepository.saveProgress(
                mediaId = entry.id,
                progress = updatedEntry.chaptersProgress,
                status = updatedEntry.status.toMediaListStatus(),
                progressVolumes = updatedEntry.volumesProgress,
            )
            if (result is NetworkResult.Failure) {
                val currentIndex = backing.indexOfFirst { it.id == entry.id }
                if (currentIndex != -1) {
                    backing[currentIndex] = originalEntry
                }
            }
        }
    }

    fun entriesInSection(section: MangaListSection): List<MangaListEntry> {
        val filtered = backing.filter { it.status == section }
        val sorted = when (currentSortOption) {
            UserListSort.FOLLOW_LIST -> filtered.sortedBy { it.originalIndex }
            UserListSort.TITLE -> filtered.sortedBy { it.title }
            UserListSort.SCORE -> filtered.sortedBy { it.score }
            UserListSort.PROGRESS -> filtered.sortedBy { it.chaptersProgress } // Strictly by chapter!
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

    fun countInSection(section: MangaListSection): Int =
        if (section == MangaListSection.ALL) backing.size
        else backing.count { it.status == section }
}

@Composable
internal fun rememberMangaListStateHolder(
    repository: MangaListRepository,
    entryRepository: MediaListEntryRepository,
    viewerId: Int,
): MangaListStateHolder {
    val scope = rememberCoroutineScope()
    return remember(viewerId) {
        MangaListStateHolder(repository, entryRepository, scope)
    }
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
