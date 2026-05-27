package com.luum.michi.app.animation.presentation.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.model.MediaReleaseDateTime

@Immutable
internal data class AnimationListEntry(
    val id: Int,
    val title: String,
    val format: String,
    val status: AnimationListSection,
    val progress: Int,
    val totalEpisodes: Int?,
    val score: String,
    val nextEpisodeRelease: MediaReleaseDateTime?,
    val palette: List<Color>,
    val coverUrl: String? = null,
    val originalIndex: Int = 0,
    val scoreDouble: Double = 0.0,
    val updatedAt: Long = 0L,
    val startedAtInt: Int = 0,
    val completedAtInt: Int = 0,
    val releaseDateInt: Int = 0,
    val averageScore: Int = 0,
    val popularity: Int = 0,
    val favouritesCount: Int = 0,
    val trending: Int = 0,
    val priority: Int = 0,
    val nextAiringAt: Long = 0L,
    val nextEpisodeNumber: Int? = null,
)

internal fun AnimationListEntry.progressLabel(): String {
    val total = totalEpisodes?.toString() ?: "?"
    return "$progress / $total"
}

internal fun AnimationListEntry.progressRatio(): Float {
    val total = totalEpisodes ?: return 0f
    return if (total == 0) 0f else (progress.toFloat() / total).coerceIn(0f, 1f)
}

internal fun AnimationListEntry.canIncrement(): Boolean {
    val total = totalEpisodes
    return total == null || progress < total
}

internal fun AnimationListEntry.releaseLabel(strings: LanguageStrings): String? {
    return nextEpisodeRelease?.let { release ->
        strings.nextEpisodeReleaseLabel(episodeNumber = nextEpisodeNumber ?: (progress + 1), releaseDateTime = release)
    }
}

internal fun AnimationListEntry.behindLabel(strings: LanguageStrings): String? {
    if (status != AnimationListSection.WATCHING) return null

    val nextEpisodeNum = nextEpisodeNumber
    val behind = if (nextEpisodeNum != null) {
        // Si está en emisión, los episodios ya emitidos son nextEpisodeNum - 1
        (nextEpisodeNum - 1) - progress
    } else {
        // Si finalizó o no hay información de estreno, usamos el total de episodios
        val total = totalEpisodes
        if (total != null) total - progress else 0
    }

    return if (behind > 0) {
        strings.episodesBehind(behind)
    } else {
        null
    }
}

internal fun AnimationListEntry.incremented(): AnimationListEntry {
    val total = totalEpisodes
    val nextProgress = if (total == null) progress + 1 else minOf(progress + 1, total)
    val nextStatus = if (total != null && nextProgress >= total) completedSectionForFormat() else status
    return copy(progress = nextProgress, status = nextStatus)
}

private fun AnimationListEntry.completedSectionForFormat(): AnimationListSection = when {
    format.startsWith("Movie") -> AnimationListSection.COMPLETED_MOVIE
    format.startsWith("OVA") -> AnimationListSection.COMPLETED_OVA
    format.startsWith("ONA") -> AnimationListSection.COMPLETED_ONA
    format.startsWith("TV Short") -> AnimationListSection.COMPLETED_TV_SHORT
    format.startsWith("Special") -> AnimationListSection.COMPLETED_SPECIAL
    else -> AnimationListSection.COMPLETED_TV
}
