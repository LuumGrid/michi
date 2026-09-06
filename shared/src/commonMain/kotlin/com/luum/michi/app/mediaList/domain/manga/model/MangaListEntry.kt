package com.luum.michi.app.mediaList.domain.manga.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.luum.michi.app.core.anilist.MediaFormat
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.model.MediaReleaseDateTime

@Immutable
internal data class MangaListEntry(
    val id: Int,
    val title: String,
    val format: MediaFormat,
    val status: MangaListSection,
    val chaptersProgress: Int,
    val totalChapters: Int?,
    val volumesProgress: Int,
    val totalVolumes: Int?,
    /** True when progress is tracked by volumes (e.g. novels) instead of chapters. Computed once in the mapper. */
    val tracksByVolume: Boolean = false,
    val score: Double,
    val nextChapterRelease: MediaReleaseDateTime?,
    val palette: List<Color>,
    val coverUrl: String? = null,
    val originalIndex: Int = 0,
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
)

internal fun MangaListEntry.chaptersProgressLabel(): String {
    val total = totalChapters?.toString() ?: "?"
    return "$chaptersProgress / $total"
}

/** Single formatted representation of the raw [MangaListEntry.score], derived at the point of use. */
internal fun MangaListEntry.formattedScore(): String {
    if (score <= 0.0) return "-"
    if (score == score.toLong().toDouble()) return score.toLong().toString()
    return ((score * 10).toLong() / 10.0).toString()
}

internal fun MangaListEntry.volumesProgressLabel(): String {
    val total = totalVolumes?.toString() ?: "?"
    return "$volumesProgress / $total"
}

internal fun MangaListEntry.chaptersProgressRatio(): Float {
    val total = totalChapters ?: return 0f
    return if (total == 0) 0f else (chaptersProgress.toFloat() / total).coerceIn(0f, 1f)
}

internal fun MangaListEntry.canIncrementChapters(): Boolean {
    val total = totalChapters
    return total == null || chaptersProgress < total
}

internal fun MangaListEntry.canIncrementVolumes(): Boolean {
    val total = totalVolumes
    return total == null || volumesProgress < total
}

/** Whether the entry is fully read, evaluating the chapters or volumes total according to [tracksByVolume]. */
internal fun MangaListEntry.isComplete(): Boolean =
    if (tracksByVolume) !canIncrementVolumes() else !canIncrementChapters()

internal fun MangaListEntry.releaseLabel(strings: LanguageStrings): String? {
    return nextChapterRelease?.let { release ->
        if (tracksByVolume) {
            strings.nextVolumeReleaseLabel(volumeNumber = volumesProgress + 1, releaseDateTime = release)
        } else {
            strings.nextChapterReleaseLabel(chapterNumber = chaptersProgress + 1, releaseDateTime = release)
        }
    }
}

internal fun MangaListEntry.behindLabel(strings: LanguageStrings): String? {
    val total = totalChapters
    val behind = if (total != null) total - chaptersProgress else 0
    return if (status == MangaListSection.CURRENT && behind > 0) {
        strings.chaptersBehind(behind)
    } else {
        null
    }
}

internal fun MangaListEntry.incrementedChapters(): MangaListEntry {
    val total = totalChapters
    val nextProgress = if (total == null) chaptersProgress + 1 else minOf(chaptersProgress + 1, total)
    val nextStatus = if (total != null && nextProgress >= total) MangaListSection.COMPLETED else status
    return copy(chaptersProgress = nextProgress, status = nextStatus)
}

internal fun MangaListEntry.incrementedVolumes(): MangaListEntry {
    val total = totalVolumes
    val nextProgress = if (total == null) volumesProgress + 1 else minOf(volumesProgress + 1, total)
    val nextStatus = if (total != null && nextProgress >= total) MangaListSection.COMPLETED else status
    return copy(volumesProgress = nextProgress, status = nextStatus)
}
