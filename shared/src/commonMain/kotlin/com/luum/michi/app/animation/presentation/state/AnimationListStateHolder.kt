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

internal class AnimationListStateHolder(
    private val repository: AnimationListRepository,
    private val scope: CoroutineScope,
) {
    private val backing = mutableStateListOf<AnimationListEntry>()
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)

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

    fun load(userId: Int) {
        scope.launch {
            loadingState = true
            errorState = null
            when (val result = repository.loadList(userId)) {
                is NetworkResult.Success -> {
                    backing.clear()
                    backing.addAll(result.value)
                }
                is NetworkResult.Failure -> {
                    errorState = result.error.toString()
                }
            }
            loadingState = false
        }
    }

    fun incrementProgress(entry: AnimationListEntry) {
        val index = backing.indexOfFirst { it.id == entry.id }
        if (index != -1) backing[index] = backing[index].incremented()
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
    viewerId: Int,
): AnimationListStateHolder {
    val scope = rememberCoroutineScope()
    return remember(viewerId) {
        AnimationListStateHolder(repository, scope)
    }
}
