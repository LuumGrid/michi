package com.luum.michi.app.reading.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformSectionHeader
import com.luum.michi.app.reading.presentation.components.ReadingEditSheet
import com.luum.michi.app.reading.presentation.components.ReadingListCard
import com.luum.michi.app.reading.presentation.model.ReadingListEntry
import com.luum.michi.app.reading.presentation.model.ReadingListSection
import com.luum.michi.app.reading.presentation.model.ReadingStatusSections
import com.luum.michi.app.reading.presentation.model.label
import com.luum.michi.app.reading.presentation.state.ReadingListStateHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingScreen(
    stateHolder: ReadingListStateHolder,
    selectedSection: ReadingListSection = ReadingListSection.ALL,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
) {
    ReadingContent(
        entriesInSection = stateHolder::entriesInSection,
        selectedSection = selectedSection,
        editingEntry = stateHolder.editingEntry,
        onStartEditing = stateHolder::startEditing,
        onStopEditing = stateHolder::stopEditing,
        onIncrementChapters = stateHolder::incrementChapters,
        onIncrementVolumes = stateHolder::incrementVolumes,
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingContent(
    entriesInSection: (ReadingListSection) -> List<ReadingListEntry>,
    selectedSection: ReadingListSection,
    editingEntry: ReadingListEntry?,
    onStartEditing: (ReadingListEntry) -> Unit,
    onStopEditing: () -> Unit,
    onIncrementChapters: (ReadingListEntry) -> Unit,
    onIncrementVolumes: (ReadingListEntry) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val strings = LanguageProvider.strings

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (selectedSection == ReadingListSection.ALL) {
                ReadingStatusSections.forEach { section ->
                    val sectionEntries = entriesInSection(section)
                    if (sectionEntries.isNotEmpty()) {
                        item {
                            PlatformSectionHeader(
                                title = section.label(strings),
                                count = sectionEntries.size,
                            )
                        }
                        items(
                            items = sectionEntries,
                            key = ReadingListEntry::id,
                        ) { entry ->
                            ReadingListCard(
                                entry = entry,
                                onOpen = {},
                                onEdit = { onStartEditing(entry) },
                                onIncrementChapters = { onIncrementChapters(entry) },
                                onIncrementVolumes = { onIncrementVolumes(entry) },
                            )
                        }
                    }
                }
            } else {
                val sectionEntries = entriesInSection(selectedSection)
                items(
                    items = sectionEntries,
                    key = ReadingListEntry::id,
                ) { entry ->
                    ReadingListCard(
                        entry = entry,
                        onOpen = {},
                        onEdit = { onStartEditing(entry) },
                        onIncrementChapters = { onIncrementChapters(entry) },
                        onIncrementVolumes = { onIncrementVolumes(entry) },
                    )
                }
            }
        }

        editingEntry?.let { entry ->
            ReadingEditSheet(entry = entry, onDismiss = onStopEditing)
        }
    }
}
