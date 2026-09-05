package com.luum.michi.app.manga.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.core.platform.components.PlatformSectionHeader
import com.luum.michi.app.core.platform.components.floatingToolbarClearance
import com.luum.michi.app.core.platform.components.tabBarClearance
import com.luum.michi.app.manga.presentation.components.MangaListCard
import com.luum.michi.app.manga.domain.model.MangaListEntry
import com.luum.michi.app.manga.domain.model.MangaListSection
import com.luum.michi.app.manga.domain.model.MangaStatusSections
import com.luum.michi.app.manga.domain.model.label
import com.luum.michi.app.manga.presentation.state.MangaListStateHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MangaScreen(
    stateHolder: MangaListStateHolder,
    selectedSection: MangaListSection = MangaListSection.ALL,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onCompletionReached: (id: Int, totalProgress: Int) -> Unit,
    onRefresh: () -> Unit,
) {
    val strings = LanguageProvider.strings
    PullToRefreshBox(
        isRefreshing = stateHolder.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        MangaContent(
            entriesInSection = stateHolder::entriesInSection,
            totalEntries = stateHolder.entries.size,
            selectedSection = selectedSection,
            isLoading = stateHolder.isLoading,
            error = stateHolder.error?.let { strings.networkErrorMessage(it) },
            onIncrementChapters = stateHolder::incrementChapters,
            onIncrementVolumes = stateHolder::incrementVolumes,
            onOpenMedia = onOpenMedia,
            onEditMedia = onEditMedia,
            onCompletionReached = onCompletionReached,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MangaContent(
    entriesInSection: (MangaListSection) -> List<MangaListEntry>,
    totalEntries: Int,
    selectedSection: MangaListSection,
    isLoading: Boolean,
    error: String?,
    onIncrementChapters: (MangaListEntry) -> Unit,
    onIncrementVolumes: (MangaListEntry) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onCompletionReached: (id: Int, totalProgress: Int) -> Unit,
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
            else -> MangaContentList(
                entriesInSection = entriesInSection,
                selectedSection = selectedSection,
                onIncrementChapters = onIncrementChapters,
                onIncrementVolumes = onIncrementVolumes,
                onOpenMedia = onOpenMedia,
                onEditMedia = onEditMedia,
                onCompletionReached = onCompletionReached,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MangaContentList(
    entriesInSection: (MangaListSection) -> List<MangaListEntry>,
    selectedSection: MangaListSection,
    onIncrementChapters: (MangaListEntry) -> Unit,
    onIncrementVolumes: (MangaListEntry) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onCompletionReached: (id: Int, totalProgress: Int) -> Unit,
) {
    val strings = LanguageProvider.strings
    val showHeaders = selectedSection == MangaListSection.ALL

    val handleIncrement: (MangaListEntry) -> Unit = { entry ->
        val total = entry.totalChapters
        if (total != null && entry.chaptersProgress + 1 >= total) {
            onCompletionReached(entry.id, total)
        } else {
            onIncrementChapters(entry)
        }
    }

    val handleIncrementVolumes: (MangaListEntry) -> Unit = { entry ->
        val total = entry.totalVolumes
        if (total != null && entry.volumesProgress + 1 >= total) {
            onCompletionReached(entry.id, total)
        } else {
            onIncrementVolumes(entry)
        }
    }

    val visibleSections by remember(selectedSection) {
        derivedStateOf {
            if (selectedSection == MangaListSection.ALL) {
                MangaStatusSections.mapNotNull { section ->
                    val entries = entriesInSection(section)
                    if (entries.isEmpty()) null else section to entries
                }
            } else {
                listOf(selectedSection to entriesInSection(selectedSection))
            }
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(selectedSection) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = floatingToolbarClearance(),
            bottom = tabBarClearance(),
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        visibleSections.forEach { (section, sectionEntries) ->
            if (showHeaders) {
                item {
                    PlatformSectionHeader(
                        title = section.label(strings),
                        count = sectionEntries.size,
                    )
                }
            }
            items(
                items = sectionEntries,
                key = MangaListEntry::id,
            ) { entry ->
                MangaListCard(
                    entry = entry,
                    onOpen = { onOpenMedia(entry.id) },
                    onEdit = { onEditMedia(entry.id) },
                    onIncrementChapters = { handleIncrement(entry) },
                    onIncrementVolumes = { handleIncrementVolumes(entry) },
                )
            }
        }
    }
}
