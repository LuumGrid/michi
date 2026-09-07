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
import com.luum.michi.app.core.model.label
import com.luum.michi.app.ui.language.LanguageProvider
import com.luum.michi.app.ui.Icons
import com.luum.michi.app.ui.components.MediaListCard

@Composable
internal fun AnimeListCard(
    entry: AnimeListEntry,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onIncrementProgress: () -> Unit,
) {
    val strings = LanguageProvider.strings

    MediaListCard(
        title = entry.title,
        subtitle = entry.format.label(unknownFallback = "Anime"),
        score = entry.formattedScore(),
        primaryProgressRatio = entry.progressRatio(),
        primaryIncrementLabel = "+1 EP",
        primaryIncrementValueLabel = entry.progressLabel(),
        primaryIncrementEnabled = entry.canIncrement(),
        paletteHex = entry.paletteHex,
        coverUrl = entry.coverUrl,
        icon = Icons.Anime,
        isComplete = !entry.canIncrement(),
        releaseLabel = entry.releaseLabel(strings),
        behindLabel = entry.behindLabel(strings),
        fallbackStatusLabel = entry.status.label(strings),
        onOpen = onOpen,
        onEdit = onEdit,
        onIncrementPrimary = onIncrementProgress,
    )
}
