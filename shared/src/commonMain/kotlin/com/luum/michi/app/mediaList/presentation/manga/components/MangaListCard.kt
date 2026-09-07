package com.luum.michi.app.mediaList.presentation.manga.components

import androidx.compose.runtime.Composable
import com.luum.michi.app.ui.language.LanguageProvider
import com.luum.michi.app.ui.Icons
import com.luum.michi.app.ui.components.MediaListCard
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.behindLabel
import com.luum.michi.app.mediaList.domain.manga.model.canIncrementChapters
import com.luum.michi.app.mediaList.domain.manga.model.canIncrementVolumes
import com.luum.michi.app.mediaList.domain.manga.model.chaptersProgressLabel
import com.luum.michi.app.mediaList.domain.manga.model.chaptersProgressRatio
import com.luum.michi.app.mediaList.domain.manga.model.formattedScore
import com.luum.michi.app.mediaList.domain.manga.model.isComplete
import com.luum.michi.app.mediaList.domain.manga.model.label
import com.luum.michi.app.mediaList.domain.manga.model.releaseLabel
import com.luum.michi.app.mediaList.domain.manga.model.volumesProgressLabel
import com.luum.michi.app.core.model.label

@Composable
internal fun MangaListCard(
    entry: MangaListEntry,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onIncrementChapters: () -> Unit,
    onIncrementVolumes: () -> Unit,
) {
    val strings = LanguageProvider.strings

    MediaListCard(
        title = entry.title,
        subtitle = entry.format.label(unknownFallback = "Manga"),
        score = entry.formattedScore(),
        primaryProgressRatio = entry.chaptersProgressRatio(),
        primaryIncrementLabel = "+1 CH",
        primaryIncrementValueLabel = entry.chaptersProgressLabel(),
        primaryIncrementEnabled = entry.canIncrementChapters(),
        secondaryIncrementLabel = "+1 VO",
        secondaryIncrementValueLabel = entry.volumesProgressLabel(),
        secondaryIncrementEnabled = entry.canIncrementVolumes(),
        paletteHex = entry.paletteHex,
        coverUrl = entry.coverUrl,
        icon = Icons.Manga,
        isComplete = entry.isComplete(),
        releaseLabel = entry.releaseLabel(strings),
        behindLabel = entry.behindLabel(strings),
        fallbackStatusLabel = entry.status.label(strings),
        onOpen = onOpen,
        onEdit = onEdit,
        onIncrementPrimary = onIncrementChapters,
        onIncrementSecondary = onIncrementVolumes,
    )
}
