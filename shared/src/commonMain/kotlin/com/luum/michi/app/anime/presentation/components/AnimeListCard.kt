package com.luum.michi.app.anime.presentation.components

import androidx.compose.runtime.Composable
import com.luum.michi.app.anime.domain.model.AnimeListEntry
import com.luum.michi.app.anime.domain.model.behindLabel
import com.luum.michi.app.anime.domain.model.canIncrement
import com.luum.michi.app.anime.domain.model.formattedScore
import com.luum.michi.app.anime.domain.model.label
import com.luum.michi.app.anime.domain.model.progressLabel
import com.luum.michi.app.anime.domain.model.progressRatio
import com.luum.michi.app.anime.domain.model.releaseLabel
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
        subtitle = entry.format.label(),
        score = entry.formattedScore(),
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
