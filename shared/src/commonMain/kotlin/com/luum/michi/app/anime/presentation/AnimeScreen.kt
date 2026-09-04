package com.luum.michi.app.anime.presentation

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
import com.luum.michi.app.anime.presentation.components.AnimeListCard
import com.luum.michi.app.anime.presentation.model.AnimeListEntry
import com.luum.michi.app.anime.presentation.model.AnimeListSection
import com.luum.michi.app.anime.presentation.model.AnimeStatusSections
import com.luum.michi.app.anime.presentation.model.label
import com.luum.michi.app.anime.presentation.state.AnimeListStateHolder
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone
import com.luum.michi.app.core.platform.components.PlatformSectionHeader
import com.luum.michi.app.core.platform.components.floatingToolbarClearance
import com.luum.michi.app.core.platform.components.tabBarClearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnimeScreen(
    stateHolder: AnimeListStateHolder,
    selectedSection: AnimeListSection,
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
        AnimeContent(
            entriesInSection = stateHolder::entriesInSection,
            totalEntries = stateHolder.entries.size,
            selectedSection = selectedSection,
            isLoading = stateHolder.isLoading,
            error = stateHolder.error?.let { strings.networkErrorMessage(it) },
            onIncrementProgress = stateHolder::incrementProgress,
            onOpenMedia = onOpenMedia,
            onEditMedia = onEditMedia,
            onCompletionReached = onCompletionReached,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimeContent(
    entriesInSection: (AnimeListSection) -> List<AnimeListEntry>,
    totalEntries: Int,
    selectedSection: AnimeListSection,
    isLoading: Boolean,
    error: String?,
    onIncrementProgress: (AnimeListEntry) -> Unit,
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
            else -> AnimeContentList(
                entriesInSection = entriesInSection,
                selectedSection = selectedSection,
                onIncrementProgress = onIncrementProgress,
                onOpenMedia = onOpenMedia,
                onEditMedia = onEditMedia,
                onCompletionReached = onCompletionReached,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimeContentList(
    entriesInSection: (AnimeListSection) -> List<AnimeListEntry>,
    selectedSection: AnimeListSection,
    onIncrementProgress: (AnimeListEntry) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onCompletionReached: (id: Int, totalProgress: Int) -> Unit,
) {
    val strings = LanguageProvider.strings

    val handleIncrement: (AnimeListEntry) -> Unit = { entry ->
        val total = entry.totalEpisodes
        if (total != null && entry.progress + 1 >= total) {
            onCompletionReached(entry.id, total)
        } else {
            onIncrementProgress(entry)
        }
    }

    val visibleSections by remember(selectedSection) {
        derivedStateOf {
            if (selectedSection == AnimeListSection.ALL) {
                AnimeStatusSections.mapNotNull { section ->
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
            item {
                PlatformSectionHeader(
                    title = section.label(strings),
                    count = sectionEntries.size,
                )
            }
            items(
                items = sectionEntries,
                key = AnimeListEntry::id,
            ) { entry ->
                AnimeListCard(
                    entry = entry,
                    onOpen = { onOpenMedia(entry.id) },
                    onEdit = { onEditMedia(entry.id) },
                    onIncrementProgress = { handleIncrement(entry) },
                )
            }
        }
    }
}