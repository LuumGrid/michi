package com.luum.michi.app.reading.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.core.platform.components.PlatformSectionHeader
import com.luum.michi.app.core.platform.components.bottomNavBarClearance
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
    searchQuery: String = "",
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onCompletionReached: (id: Int, totalProgress: Int) -> Unit,
    onSearchGlobally: () -> Unit = {},
    onRefresh: () -> Unit,
) {
    val strings = LanguageProvider.strings
    PullToRefreshBox(
        isRefreshing = stateHolder.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        ReadingContent(
            entriesInSection = stateHolder::entriesInSection,
            totalEntries = stateHolder.entries.size,
            selectedSection = selectedSection,
            searchQuery = searchQuery,
            isLoading = stateHolder.isLoading,
            error = stateHolder.error?.let { strings.networkErrorMessage(it) },
            onIncrementChapters = stateHolder::incrementChapters,
            onIncrementVolumes = stateHolder::incrementVolumes,
            onOpenMedia = onOpenMedia,
            onEditMedia = onEditMedia,
            onCompletionReached = onCompletionReached,
            onSearchGlobally = onSearchGlobally,
            scrollBehavior = scrollBehavior,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingContent(
    entriesInSection: (ReadingListSection) -> List<ReadingListEntry>,
    totalEntries: Int,
    selectedSection: ReadingListSection,
    searchQuery: String,
    isLoading: Boolean,
    error: String?,
    onIncrementChapters: (ReadingListEntry) -> Unit,
    onIncrementVolumes: (ReadingListEntry) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onCompletionReached: (id: Int, totalProgress: Int) -> Unit,
    onSearchGlobally: () -> Unit,
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
                searchQuery = searchQuery,
                scrollBehavior = scrollBehavior,
                onIncrementChapters = onIncrementChapters,
                onIncrementVolumes = onIncrementVolumes,
                onOpenMedia = onOpenMedia,
                onEditMedia = onEditMedia,
                onCompletionReached = onCompletionReached,
                onSearchGlobally = onSearchGlobally,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingContentList(
    entriesInSection: (ReadingListSection) -> List<ReadingListEntry>,
    selectedSection: ReadingListSection,
    searchQuery: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onIncrementChapters: (ReadingListEntry) -> Unit,
    onIncrementVolumes: (ReadingListEntry) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onCompletionReached: (id: Int, totalProgress: Int) -> Unit,
    onSearchGlobally: () -> Unit,
) {
    val strings = LanguageProvider.strings
    val query = searchQuery.trim()
    val isSearching = query.isNotEmpty()
    val showHeaders = isSearching || selectedSection == ReadingListSection.ALL

    val handleIncrement: (ReadingListEntry) -> Unit = { entry ->
        val total = entry.totalChapters
        if (total != null && entry.chaptersProgress + 1 >= total) {
            onCompletionReached(entry.id, total)
        } else {
            onIncrementChapters(entry)
        }
    }

    val visibleSections by remember(selectedSection, query) {
        derivedStateOf {
            if (isSearching || selectedSection == ReadingListSection.ALL) {
                ReadingStatusSections.mapNotNull { section ->
                    val entries = entriesInSection(section).let { list ->
                        if (isSearching) list.filter { it.title.contains(query, ignoreCase = true) } else list
                    }
                    if (entries.isEmpty()) null else section to entries
                }
            } else {
                listOf(selectedSection to entriesInSection(selectedSection))
            }
        }
    }

    if (isSearching && visibleSections.isEmpty()) {
        PlatformListMessage(
            title = strings.searchNoResultsLabel,
            actionLabel = strings.searchGloballyAction,
            onAction = onSearchGlobally,
        )
        return
    }

    val listState = rememberLazyListState()

    LaunchedEffect(selectedSection) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 14.dp, bottom = bottomNavBarClearance()),
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
                key = ReadingListEntry::id,
            ) { entry ->
                ReadingListCard(
                    entry = entry,
                    onOpen = { onOpenMedia(entry.id) },
                    onEdit = { onEditMedia(entry.id) },
                    onIncrementChapters = { handleIncrement(entry) },
                    onIncrementVolumes = { onIncrementVolumes(entry) },
                )
            }
        }
    }
}
