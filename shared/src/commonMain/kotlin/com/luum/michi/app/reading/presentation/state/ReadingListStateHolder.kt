package com.luum.michi.app.reading.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.reading.data.ReadingListRepository
import com.luum.michi.app.reading.presentation.model.ReadingListEntry
import com.luum.michi.app.reading.presentation.model.ReadingListSection
import com.luum.michi.app.reading.presentation.model.incrementedChapters
import com.luum.michi.app.reading.presentation.model.incrementedVolumes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ReadingListStateHolder(
    private val repository: ReadingListRepository,
    private val scope: CoroutineScope,
) {
    private val backing = mutableStateListOf<ReadingListEntry>()
    private var loadingState by mutableStateOf(false)
    private var errorState by mutableStateOf<String?>(null)

    val entries: List<ReadingListEntry> get() = backing
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

    fun incrementChapters(entry: ReadingListEntry) {
        val index = backing.indexOfFirst { it.id == entry.id }
        if (index != -1) backing[index] = backing[index].incrementedChapters()
    }

    fun incrementVolumes(entry: ReadingListEntry) {
        val index = backing.indexOfFirst { it.id == entry.id }
        if (index != -1) backing[index] = backing[index].incrementedVolumes()
    }

    fun entriesInSection(section: ReadingListSection): List<ReadingListEntry> =
        backing.filter { it.status == section }

    fun countInSection(section: ReadingListSection): Int =
        if (section == ReadingListSection.ALL) backing.size
        else backing.count { it.status == section }
}

@Composable
internal fun rememberReadingListStateHolder(
    repository: ReadingListRepository,
    viewerId: Int,
): ReadingListStateHolder {
    val scope = rememberCoroutineScope()
    return remember(viewerId) {
        ReadingListStateHolder(repository, scope)
    }
}
