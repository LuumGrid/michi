package com.luum.michi.app.mediaList.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.SectionHeader
import com.luum.michi.app.ui.components.floatingToolbarClearance
import com.luum.michi.app.ui.components.tabBarClearance

/**
 * Shared sectioned list for the anime/manga user lists: groups [entriesInSection]
 * output by section (skipping empty ones when showing all), resets scroll on
 * section change, and renders headers + cards. Each screen only provides its
 * [itemContent] (the single part that genuinely differs).
 */
@Composable
internal fun <S, T : Any> MediaListScaffold(
    selectedSection: S,
    allSections: List<S>,
    isAllSelected: Boolean,
    showHeaders: Boolean,
    entriesInSection: (S) -> List<T>,
    headerTitle: (S) -> String,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
) {
    val visibleSections by remember(selectedSection) {
        derivedStateOf {
            if (isAllSelected) {
                allSections.mapNotNull { section ->
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
                    SectionHeader(
                        title = headerTitle(section),
                        count = sectionEntries.size,
                    )
                }
            }
            items(
                items = sectionEntries,
                key = itemKey,
            ) { entry ->
                itemContent(entry)
            }
        }
    }
}
