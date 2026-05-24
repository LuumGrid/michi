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

internal class AnimationListStateHolder(
    private val repository: AnimationListRepository,
    private val scope: CoroutineScope,
) {
    private val backing = mutableStateListOf<AnimationListEntry>()
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)

    val entries: List<AnimationListEntry> get() = backing
    val isLoading: Boolean get() = loadingState
    val error: String? get() = errorState

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

    fun entriesInSection(section: AnimationListSection): List<AnimationListEntry> =
        backing.filter { it.status == section }

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
