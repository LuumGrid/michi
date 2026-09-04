package com.luum.michi.app.anime.presentation.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.model.MediaReleaseDateTime

@Immutable
internal data class AnimeListEntry(
    val id: Int,
    val title: String,
    val format: String,
    val status: AnimeListSection,
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

internal fun AnimeListEntry.progressLabel(): String {
    val total = totalEpisodes?.toString() ?: "?"
    return "$progress / $total"
}

internal fun AnimeListEntry.progressRatio(): Float {
    val total = totalEpisodes ?: return 0f
    return if (total == 0) 0f else (progress.toFloat() / total).coerceIn(0f, 1f)
}

internal fun AnimeListEntry.canIncrement(): Boolean {
    val total = totalEpisodes
    return total == null || progress < total
}

internal fun AnimeListEntry.releaseLabel(strings: LanguageStrings): String? {
    return nextEpisodeRelease?.let { release ->
        strings.nextEpisodeReleaseLabel(episodeNumber = nextEpisodeNumber ?: (progress + 1), releaseDateTime = release)
    }
}

internal fun AnimeListEntry.behindLabel(strings: LanguageStrings): String? {
    if (status != AnimeListSection.WATCHING) return null

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

internal fun AnimeListEntry.incremented(): AnimeListEntry {
    val total = totalEpisodes
    val nextProgress = if (total == null) progress + 1 else minOf(progress + 1, total)
    val nextStatus = if (total != null && nextProgress >= total) completedSectionForFormat() else status
    return copy(progress = nextProgress, status = nextStatus)
}

private fun AnimeListEntry.completedSectionForFormat(): AnimeListSection = when {
    format.startsWith("Movie") -> AnimeListSection.COMPLETED_MOVIE
    format.startsWith("OVA") -> AnimeListSection.COMPLETED_OVA
    format.startsWith("ONA") -> AnimeListSection.COMPLETED_ONA
    format.startsWith("TV Short") -> AnimeListSection.COMPLETED_TV_SHORT
    format.startsWith("Special") -> AnimeListSection.COMPLETED_SPECIAL
    else -> AnimeListSection.COMPLETED_TV
}
