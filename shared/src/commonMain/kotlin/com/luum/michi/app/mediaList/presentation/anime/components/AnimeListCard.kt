package com.luum.michi.app.mediaList.presentation.anime.components

import androidx.compose.runtime.Composable
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.behindLabel
import com.luum.michi.app.mediaList.domain.anime.model.canIncrement
import com.luum.michi.app.mediaList.domain.anime.model.formattedScore
import com.luum.michi.app.mediaList.domain.anime.model.label
import com.luum.michi.app.mediaList.domain.anime.model.progressLabel
import com.luum.michi.app.mediaList.domain.anime.model.progressRatio
import com.luum.michi.app.mediaList.domain.anime.model.releaseLabel
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
