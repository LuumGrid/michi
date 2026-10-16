package com.luum.michi.app.mediaList.domain.anime.model

import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.MediaSeason
import com.luum.michi.app.core.model.MediaWorkStatus
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.MediaReleaseDateTime
import com.luum.michi.app.mediaList.domain.common.SortableMediaListEntry
import com.luum.michi.app.core.model.CoverSizes

internal data class AnimeListEntry(
    override val id: Int,
    override val title: String,
    /** Every non-blank title variant (romaji/english/native): search matches any of them. */
    val titles: List<String> = listOf(title),
    override val format: MediaFormat,
    val mediaStatus: MediaWorkStatus? = null,
    val status: AnimeListSection,
    val progress: Int,
    val totalEpisodes: Int?,
    override val score: Double,
    val nextEpisodeRelease: MediaReleaseDateTime?,
    val paletteHex: String?,
    val cover: CoverSizes? = null,
    override val originalIndex: Int = 0,
    override val updatedAt: Long = 0L,
    override val startedAtInt: Int = 0,
    override val completedAtInt: Int = 0,
    override val releaseDateInt: Int = 0,
    override val averageScore: Int = 0,
    override val popularity: Int = 0,
    override val favouritesCount: Int = 0,
    override val trending: Int = 0,
    override val priority: Int = 0,
    val nextAiringAt: Long = 0L,
    val nextEpisodeNumber: Int? = null,
    override val genres: List<String> = emptyList(),
    override val isAdult: Boolean = false,
    override val season: MediaSeason? = null,
    override val seasonYear: Int? = null,
) : SortableMediaListEntry

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

internal fun AnimeListEntry.incremented(splitCompleted: Boolean): AnimeListEntry {
    val total = totalEpisodes
    val nextProgress = if (total == null) progress + 1 else minOf(progress + 1, total)
    // Same section rule as the mapper: a per-format COMPLETED only when split is
    // on, otherwise the merged one the sections list actually shows.
    val nextStatus = if (total != null && nextProgress >= total) {
        if (splitCompleted) format.completedSection() else AnimeListSection.COMPLETED
    } else {
        status
    }
    return copy(progress = nextProgress, status = nextStatus)
}
