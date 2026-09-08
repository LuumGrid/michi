package com.luum.michi.app.mediaList.domain.manga.model

import com.luum.michi.app.core.domain.model.MediaFormat
import com.luum.michi.app.core.domain.model.MediaSeason
import com.luum.michi.app.core.domain.language.LanguageStrings
import com.luum.michi.app.mediaList.domain.common.SortableMediaListEntry

internal data class MangaListEntry(
    override val id: Int,
    override val title: String,
    override val format: MediaFormat,
    val status: MangaListSection,
    val chaptersProgress: Int,
    val totalChapters: Int?,
    val volumesProgress: Int,
    val totalVolumes: Int?,
    /** True when progress is tracked by volumes (e.g. novels) instead of chapters. Computed once in the mapper. */
    val tracksByVolume: Boolean = false,
    override val score: Double,
    val paletteHex: String?,
    val coverUrl: String? = null,
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
    override val genres: List<String> = emptyList(),
    override val season: MediaSeason? = null,
    override val seasonYear: Int? = null,
) : SortableMediaListEntry

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
