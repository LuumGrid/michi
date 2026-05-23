package com.luum.michi.app.reading.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luum.michi.app.reading.presentation.model.ReadingListEntry
import com.luum.michi.app.reading.presentation.model.ReadingListSection
import com.luum.michi.app.reading.presentation.model.incrementedChapters
import com.luum.michi.app.reading.presentation.model.incrementedVolumes

internal class ReadingListStateHolder(initialEntries: List<ReadingListEntry> = emptyList()) {
    private val backing = mutableStateListOf<ReadingListEntry>().apply { addAll(initialEntries) }
    private var editingState by mutableStateOf<ReadingListEntry?>(null)

    val entries: List<ReadingListEntry> get() = backing
    val editingEntry: ReadingListEntry? get() = editingState

    fun startEditing(entry: ReadingListEntry) {
        editingState = entry
    }

    fun stopEditing() {
        editingState = null
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
internal fun rememberReadingListStateHolder(): ReadingListStateHolder =
    remember { ReadingListStateHolder() }
