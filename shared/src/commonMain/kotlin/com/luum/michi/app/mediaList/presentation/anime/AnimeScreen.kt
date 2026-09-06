package com.luum.michi.app.mediaList.presentation.anime

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luum.michi.app.mediaList.presentation.anime.components.AnimeListCard
import com.luum.michi.app.mediaList.presentation.common.MediaListScaffold
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.AnimeStatusSections
import com.luum.michi.app.mediaList.domain.anime.model.label
import com.luum.michi.app.mediaList.presentation.anime.state.AnimeListStateHolder
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone

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

    MediaListScaffold(
        selectedSection = selectedSection,
        allSections = AnimeStatusSections,
        isAllSelected = selectedSection == AnimeListSection.ALL,
        showHeaders = true,
        entriesInSection = entriesInSection,
        headerTitle = { it.label(strings) },
        itemKey = AnimeListEntry::id,
    ) { entry ->
        AnimeListCard(
            entry = entry,
            onOpen = { onOpenMedia(entry.id) },
            onEdit = { onEditMedia(entry.id) },
            onIncrementProgress = { handleIncrement(entry) },
        )
    }
}