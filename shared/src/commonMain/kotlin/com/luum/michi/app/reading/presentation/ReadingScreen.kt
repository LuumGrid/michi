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
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.core.platform.components.PlatformSectionHeader
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
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
) {
    ReadingContent(
        entriesInSection = stateHolder::entriesInSection,
        totalEntries = stateHolder.entries.size,
        selectedSection = selectedSection,
        isLoading = stateHolder.isLoading,
        error = stateHolder.error,
        onIncrementChapters = stateHolder::incrementChapters,
        onIncrementVolumes = stateHolder::incrementVolumes,
        onOpenMedia = onOpenMedia,
        onEditMedia = onEditMedia,
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingContent(
    entriesInSection: (ReadingListSection) -> List<ReadingListEntry>,
    totalEntries: Int,
    selectedSection: ReadingListSection,
    isLoading: Boolean,
    error: String?,
    onIncrementChapters: (ReadingListEntry) -> Unit,
    onIncrementVolumes: (ReadingListEntry) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val strings = LanguageProvider.strings

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && totalEntries == 0 -> PlatformListLoading(strings.listsLoadingLabel)
            error != null && totalEntries == 0 -> PlatformListMessage(
                title = strings.listsErrorLabel,
                subtitle = error,
                tone = PlatformListMessageTone.Error,
            )
            totalEntries == 0 -> PlatformListMessage(title = strings.listsEmptyLabel)
            else -> ReadingContentList(
                entriesInSection = entriesInSection,
                selectedSection = selectedSection,
                scrollBehavior = scrollBehavior,
                onIncrementChapters = onIncrementChapters,
                onIncrementVolumes = onIncrementVolumes,
                onOpenMedia = onOpenMedia,
                onEditMedia = onEditMedia,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingContentList(
    entriesInSection: (ReadingListSection) -> List<ReadingListEntry>,
    selectedSection: ReadingListSection,
    scrollBehavior: TopAppBarScrollBehavior,
    onIncrementChapters: (ReadingListEntry) -> Unit,
    onIncrementVolumes: (ReadingListEntry) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 96.dp),
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
                            onOpen = { onOpenMedia(entry.id) },
                            onEdit = { onEditMedia(entry.id) },
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
                    onOpen = { onOpenMedia(entry.id) },
                    onEdit = { onEditMedia(entry.id) },
                    onIncrementChapters = { onIncrementChapters(entry) },
                    onIncrementVolumes = { onIncrementVolumes(entry) },
                )
            }
        }
    }
}
