package com.luum.michi.app.animation.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.luum.michi.app.animation.presentation.components.AnimationListCard
import com.luum.michi.app.animation.presentation.model.AnimationListEntry
import com.luum.michi.app.animation.presentation.model.AnimationListSection
import com.luum.michi.app.animation.presentation.model.AnimationStatusSections
import com.luum.michi.app.animation.presentation.model.label
import com.luum.michi.app.animation.presentation.state.AnimationListStateHolder
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnimationScreen(
    stateHolder: AnimationListStateHolder,
    selectedSection: AnimationListSection,
    searchQuery: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onCompletionReached: (id: Int, totalProgress: Int) -> Unit,
    onSearchGlobally: () -> Unit,
) {
    AnimationContent(
        entriesInSection = stateHolder::entriesInSection,
        selectedSection = selectedSection,
        searchQuery = searchQuery,
        onIncrementProgress = stateHolder::incrementProgress,
        onOpenMedia = onOpenMedia,
        onEditMedia = onEditMedia,
        onCompletionReached = onCompletionReached,
        onSearchGlobally = onSearchGlobally,
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimationContent(
    entriesInSection: (AnimationListSection) -> List<AnimationListEntry>,
    selectedSection: AnimationListSection,
    searchQuery: String,
    onIncrementProgress: (AnimationListEntry) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onCompletionReached: (id: Int, totalProgress: Int) -> Unit,
    onSearchGlobally: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val strings = LanguageProvider.strings
    val query = searchQuery.trim()
    val isSearching = query.isNotEmpty()

    val handleIncrement: (AnimationListEntry) -> Unit = { entry ->
        val total = entry.totalEpisodes
        if (total != null && entry.progress + 1 >= total) {
            onCompletionReached(entry.id, total)
        } else {
            onIncrementProgress(entry)
        }
    }

    val visibleSections: List<Pair<AnimationListSection, List<AnimationListEntry>>> =
        if (isSearching || selectedSection == AnimationListSection.ALL) {
            AnimationStatusSections.mapNotNull { section ->
                val entries = entriesInSection(section).let { list ->
                    if (isSearching) list.filter { it.title.contains(query, ignoreCase = true) } else list
                }
                if (entries.isEmpty()) null else section to entries
            }
        } else {
            listOf(selectedSection to entriesInSection(selectedSection))
        }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isSearching && visibleSections.isEmpty()) {
            PlatformListMessage(
                title = strings.searchNoResultsLabel,
                actionLabel = strings.searchGloballyAction,
                onAction = onSearchGlobally,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 96.dp),
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
                        key = AnimationListEntry::id,
                    ) { entry ->
                        AnimationListCard(
                            entry = entry,
                            onOpen = { onOpenMedia(entry.id) },
                            onEdit = { onEditMedia(entry.id) },
                            onIncrementProgress = { handleIncrement(entry) },
                        )
                    }
                }
            }
        }
    }
}
