package com.luum.michi.app.animation.presentation.components

import androidx.compose.runtime.Composable
import com.luum.michi.app.animation.presentation.model.AnimationListEntry
import com.luum.michi.app.animation.presentation.model.behindLabel
import com.luum.michi.app.animation.presentation.model.canIncrement
import com.luum.michi.app.animation.presentation.model.label
import com.luum.michi.app.animation.presentation.model.progressLabel
import com.luum.michi.app.animation.presentation.model.progressRatio
import com.luum.michi.app.animation.presentation.model.releaseLabel
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformMediaListCard

@Composable
internal fun AnimationListCard(
    entry: AnimationListEntry,
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
        icon = PlatformIcons.Animation,
        isComplete = !entry.canIncrement(),
        releaseLabel = entry.releaseLabel(strings),
        behindLabel = entry.behindLabel(strings),
        fallbackStatusLabel = entry.status.label(strings),
        onOpen = onOpen,
        onEdit = onEdit,
        onIncrementPrimary = onIncrementProgress,
    )
}
