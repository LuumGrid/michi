package com.luum.michi.app.anime.presentation.components

import androidx.compose.runtime.Composable
import com.luum.michi.app.anime.presentation.model.AnimeListEntry
import com.luum.michi.app.anime.presentation.model.behindLabel
import com.luum.michi.app.anime.presentation.model.canIncrement
import com.luum.michi.app.anime.presentation.model.label
import com.luum.michi.app.anime.presentation.model.progressLabel
import com.luum.michi.app.anime.presentation.model.progressRatio
import com.luum.michi.app.anime.presentation.model.releaseLabel
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformMediaListCard

@Composable
internal fun AnimeListCard(
    entry: AnimeListEntry,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onIncrementProgress: () -> Unit,
) {
    val strings = LanguageProvider.strings

    PlatformMediaListCard(
        title = entry.title,
        subtitle = entry.format,
        score = entry.score,
        primaryProgressLabel = entry.progressLabel(),
        primaryProgressRatio = entry.progressRatio(),
        primaryIncrementLabel = "+1 EP",
        primaryIncrementValueLabel = entry.progressLabel(),
        primaryIncrementEnabled = entry.canIncrement(),
        palette = entry.palette,
        coverUrl = entry.coverUrl,
        icon = PlatformIcons.Anime,
        isComplete = !entry.canIncrement(),
        releaseLabel = entry.releaseLabel(strings),
        behindLabel = entry.behindLabel(strings),
        fallbackStatusLabel = entry.status.label(strings),
        onOpen = onOpen,
        onEdit = onEdit,
        onIncrementPrimary = onIncrementProgress,
    )
}
