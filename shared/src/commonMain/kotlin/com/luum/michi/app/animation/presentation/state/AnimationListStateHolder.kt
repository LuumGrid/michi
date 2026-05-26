package com.luum.michi.app.animation.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.animation.data.AnimationListRepository
import com.luum.michi.app.animation.presentation.model.AnimationListEntry
import com.luum.michi.app.animation.presentation.model.AnimationListSection
import com.luum.michi.app.animation.presentation.model.incremented
import com.luum.michi.app.core.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import com.luum.michi.app.core.platform.model.UserListSort
import com.luum.michi.app.core.platform.model.UserListOrder
import com.luum.michi.app.mediaDetail.data.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.presentation.model.MediaListStatus
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

internal class AnimationListStateHolder(
    private val repository: AnimationListRepository,
    private val entryRepository: MediaListEntryRepository,
    private val scope: CoroutineScope,
) {
    private val backing = mutableStateListOf<AnimationListEntry>()
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null
    private var lastUserId: Int? = null

    var currentSortOption by mutableStateOf(UserListSort.FOLLOW_LIST)
    var currentSortOrder by mutableStateOf(UserListOrder.DESCENDING)
    var isFilterPersisted by mutableStateOf(false)

    val entries: List<AnimationListEntry> get() = backing
    val isLoading: Boolean get() = loadingState
    val error: String? get() = errorState

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
        scope.launch {
            loadingState = true
            errorState = null
            when (val result = repository.loadList(userId)) {
                is NetworkResult.Success -> {
                    backing.clear()
                    backing.addAll(result.value)
                    lastLoaded = timeMark.markNow()
                    lastUserId = userId
                }
                is NetworkResult.Failure -> {
                    errorState = result.error.toString()
                }
            }
            loadingState = false
        }
    }

    companion object {
        private val CACHE_TTL = 5.minutes
    }

    fun incrementProgress(entry: AnimationListEntry) {
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

    fun entriesInSection(section: AnimationListSection): List<AnimationListEntry> {
        val filtered = backing.filter { it.status == section }
        val sorted = when (currentSortOption) {
            UserListSort.FOLLOW_LIST -> filtered.sortedBy { it.originalIndex }
            UserListSort.TITLE -> filtered.sortedBy { it.title }
            UserListSort.SCORE -> filtered.sortedBy { it.scoreDouble }
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

    fun countInSection(section: AnimationListSection): Int =
        if (section == AnimationListSection.ALL) backing.size
        else backing.count { it.status == section }
}

@Composable
internal fun rememberAnimationListStateHolder(
    repository: AnimationListRepository,
    entryRepository: MediaListEntryRepository,
    viewerId: Int,
): AnimationListStateHolder {
    val scope = rememberCoroutineScope()
    return remember(viewerId) {
        AnimationListStateHolder(repository, entryRepository, scope)
    }
}

private fun AnimationListSection.toMediaListStatus(): MediaListStatus = when (this) {
    AnimationListSection.ALL -> MediaListStatus.CURRENT
    AnimationListSection.WATCHING -> MediaListStatus.CURRENT
    AnimationListSection.COMPLETED_TV,
    AnimationListSection.COMPLETED_MOVIE,
    AnimationListSection.COMPLETED_OVA,
    AnimationListSection.COMPLETED_ONA,
    AnimationListSection.COMPLETED_TV_SHORT,
    AnimationListSection.COMPLETED_SPECIAL -> MediaListStatus.COMPLETED
    AnimationListSection.PAUSED -> MediaListStatus.PAUSED
    AnimationListSection.DROPPED -> MediaListStatus.DROPPED
    AnimationListSection.PLANNING -> MediaListStatus.PLANNING
    AnimationListSection.REWATCHING -> MediaListStatus.REPEATING
}
