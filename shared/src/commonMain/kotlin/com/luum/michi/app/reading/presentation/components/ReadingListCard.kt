package com.luum.michi.app.reading.presentation.components

import androidx.compose.runtime.Composable
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformMediaListCard
import com.luum.michi.app.reading.presentation.model.ReadingListEntry
import com.luum.michi.app.reading.presentation.model.behindLabel
import com.luum.michi.app.reading.presentation.model.canIncrementChapters
import com.luum.michi.app.reading.presentation.model.canIncrementVolumes
import com.luum.michi.app.reading.presentation.model.chaptersProgressLabel
import com.luum.michi.app.reading.presentation.model.chaptersProgressRatio
import com.luum.michi.app.reading.presentation.model.label
import com.luum.michi.app.reading.presentation.model.releaseLabel
import com.luum.michi.app.reading.presentation.model.volumesProgressLabel

@Composable
internal fun ReadingListCard(
    entry: ReadingListEntry,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onIncrementChapters: () -> Unit,
    onIncrementVolumes: () -> Unit,
) {
    val strings = LanguageProvider.strings

    PlatformMediaListCard(
        title = entry.title,
        subtitle = entry.format,
        score = entry.score,
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
        icon = PlatformIcons.Reading,
        isComplete = !entry.canIncrementChapters(),
        releaseLabel = entry.releaseLabel(strings),
        behindLabel = entry.behindLabel(strings),
        fallbackStatusLabel = entry.status.label(strings),
        onOpen = onOpen,
        onEdit = onEdit,
        onIncrementPrimary = onIncrementChapters,
        onIncrementSecondary = onIncrementVolumes,
    )
}
