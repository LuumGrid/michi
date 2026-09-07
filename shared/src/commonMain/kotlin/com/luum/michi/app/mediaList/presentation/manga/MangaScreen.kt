package com.luum.michi.app.mediaList.presentation.manga

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luum.michi.app.ui.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.ui.components.ListLoading
import com.luum.michi.app.ui.components.ListMessage
import com.luum.michi.app.ui.components.ListMessageTone
import com.luum.michi.app.mediaList.presentation.manga.components.MangaListCard
import com.luum.michi.app.mediaList.presentation.common.MediaListScaffold
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.MangaStatusSections
import com.luum.michi.app.mediaList.domain.manga.model.label
import com.luum.michi.app.mediaList.presentation.manga.state.MangaListStateHolder

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
            isLoading && totalEntries == 0 -> ListLoading(strings.listsLoadingLabel)
            error != null && totalEntries == 0 -> ListMessage(
                title = strings.listsErrorLabel,
                subtitle = error,
                tone = ListMessageTone.Error,
            )
            totalEntries == 0 -> ListMessage(title = strings.listsEmptyLabel)
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

    MediaListScaffold(
        selectedSection = selectedSection,
        allSections = MangaStatusSections,
        isAllSelected = selectedSection == MangaListSection.ALL,
        showHeaders = selectedSection == MangaListSection.ALL,
        entriesInSection = entriesInSection,
        headerTitle = { it.label(strings) },
        itemKey = MangaListEntry::id,
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
