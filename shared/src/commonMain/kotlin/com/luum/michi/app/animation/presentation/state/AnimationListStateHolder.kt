package com.luum.michi.app.animation.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luum.michi.app.animation.presentation.model.AnimationListEntry
import com.luum.michi.app.animation.presentation.model.AnimationListSection
import com.luum.michi.app.animation.presentation.model.incremented
import com.luum.michi.app.animation.presentation.sample.AnimationSampleEntries

internal class AnimationListStateHolder(initialEntries: List<AnimationListEntry>) {
    private val backing = mutableStateListOf<AnimationListEntry>().apply { addAll(initialEntries) }
    private var editingState by mutableStateOf<AnimationListEntry?>(null)

    val entries: List<AnimationListEntry> get() = backing
    val editingEntry: AnimationListEntry? get() = editingState

    fun startEditing(entry: AnimationListEntry) {
        editingState = entry
    }

    fun stopEditing() {
        editingState = null
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
internal fun rememberAnimationListStateHolder(): AnimationListStateHolder =
    remember { AnimationListStateHolder(AnimationSampleEntries) }
