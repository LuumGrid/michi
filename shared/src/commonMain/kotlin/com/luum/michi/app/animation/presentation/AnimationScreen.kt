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
import com.luum.michi.app.core.platform.components.PlatformSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnimationScreen(
    stateHolder: AnimationListStateHolder,
    selectedSection: AnimationListSection,
    scrollBehavior: TopAppBarScrollBehavior,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
) {
    AnimationContent(
        entriesInSection = stateHolder::entriesInSection,
        selectedSection = selectedSection,
        onIncrementProgress = stateHolder::incrementProgress,
        onOpenMedia = onOpenMedia,
        onEditMedia = onEditMedia,
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimationContent(
    entriesInSection: (AnimationListSection) -> List<AnimationListEntry>,
    selectedSection: AnimationListSection,
    onIncrementProgress: (AnimationListEntry) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
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
            if (selectedSection == AnimationListSection.ALL) {
                AnimationStatusSections.forEach { section ->
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
                            key = AnimationListEntry::id,
                        ) { entry ->
                            AnimationListCard(
                                entry = entry,
                                onOpen = { onOpenMedia(entry.id) },
                                onEdit = { onEditMedia(entry.id) },
                                onIncrementProgress = { onIncrementProgress(entry) },
                            )
                        }
                    }
                }
            } else {
                val sectionEntries = entriesInSection(selectedSection)
                item {
                    PlatformSectionHeader(
                        title = selectedSection.label(strings),
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
                        onIncrementProgress = { onIncrementProgress(entry) },
                    )
                }
            }
        }
    }
}
