package com.luum.michi.app.manga.presentation.components

import androidx.compose.runtime.Composable
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformMediaListCard
import com.luum.michi.app.manga.domain.model.MangaListEntry
import com.luum.michi.app.manga.domain.model.behindLabel
import com.luum.michi.app.manga.domain.model.canIncrementChapters
import com.luum.michi.app.manga.domain.model.canIncrementVolumes
import com.luum.michi.app.manga.domain.model.chaptersProgressLabel
import com.luum.michi.app.manga.domain.model.chaptersProgressRatio
import com.luum.michi.app.manga.domain.model.formattedScore
import com.luum.michi.app.manga.domain.model.isComplete
import com.luum.michi.app.manga.domain.model.label
import com.luum.michi.app.manga.domain.model.releaseLabel
import com.luum.michi.app.manga.domain.model.volumesProgressLabel

@Composable
internal fun MangaListCard(
    entry: MangaListEntry,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onIncrementChapters: () -> Unit,
    onIncrementVolumes: () -> Unit,
) {
    val strings = LanguageProvider.strings

    PlatformMediaListCard(
        title = entry.title,
        subtitle = entry.format.label(),
        score = entry.formattedScore(),
        primaryProgressLabel = entry.chaptersProgressLabel(),
        primaryProgressRatio = entry.chaptersProgressRatio(),
        primaryIncrementLabel = "+1 CH",
        primaryIncrementValueLabel = entry.chaptersProgressLabel(),
        primaryIncrementEnabled = entry.canIncrementChapters(),
        secondaryIncrementLabel = "+1 VO",
        secondaryIncrementValueLabel = entry.volumesProgressLabel(),
        secondaryIncrementEnabled = entry.canIncrementVolumes(),
        palette = entry.palette,
        coverUrl = entry.coverUrl,
        icon = PlatformIcons.Manga,
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
