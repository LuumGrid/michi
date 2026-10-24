package com.luum.michi.app.mediaList.domain.manga.model

import com.luum.michi.app.core.media.domain.MediaFormat
import com.luum.michi.app.core.media.domain.MediaSeason
import com.luum.michi.app.core.media.domain.MediaWorkStatus
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaList.domain.common.SortableMediaListEntry
import com.luum.michi.app.core.model.CoverSizes

internal data class MangaListEntry(
    override val id: Int,
    override val title: String,
    /** Every non-blank title variant (romaji/english/native): search matches any of them. */
    override val titles: List<String> = listOf(title),
    override val format: MediaFormat,
    val mediaStatus: MediaWorkStatus? = null,
    val status: MangaListSection,
    val chaptersProgress: Int,
    val totalChapters: Int?,
    val volumesProgress: Int,
    val totalVolumes: Int?,
    /** True when progress is tracked by volumes (e.g. novels) instead of chapters. Computed once in the mapper. */
    val tracksByVolume: Boolean = false,
    override val score: Double,
    val paletteHex: String?,
    override val cover: CoverSizes? = null,
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
    override val isAdult: Boolean = false,
    override val season: MediaSeason? = null,
    override val seasonYear: Int? = null,
) : SortableMediaListEntry

internal fun MangaListEntry.chaptersProgressLabel(): String {
    val total = totalChapters?.toString() ?: "?"
    return "$chaptersProgress / $total"
}

internal fun MangaListEntry.volumesProgressLabel(): String {
    val total = totalVolumes?.toString() ?: "?"
    return "$volumesProgress / $total"
}

internal fun MangaListEntry.canIncrementChapters(): Boolean {
    val total = totalChapters
    return total == null || chaptersProgress < total
}

internal fun MangaListEntry.canIncrementVolumes(): Boolean {
    val total = totalVolumes
    return total == null || volumesProgress < total
}

/**
 * True when the next `+1` lands the entry on its chapter total. Mirrors the
 * completion rule in [incrementedChapters] exactly.
 */
internal fun MangaListEntry.completesOnChapterIncrement(): Boolean {
    val total = totalChapters
    return total != null && chaptersProgress + 1 >= total
}

/**
 * True when the next `+1` lands the entry on its volume total. Mirrors the
 * completion rule in [incrementedVolumes] exactly.
 */
internal fun MangaListEntry.completesOnVolumeIncrement(): Boolean {
    val total = totalVolumes
    return total != null && volumesProgress + 1 >= total
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

internal fun MangaListEntry.incrementedChapters(splitCompleted: Boolean): MangaListEntry {
    val total = totalChapters
    val nextProgress = if (total == null) chaptersProgress + 1 else minOf(chaptersProgress + 1, total)
    val nextStatus = if (total != null && nextProgress >= total) {
        format.completedMangaSection(splitCompleted)
    } else {
        status
    }
    return copy(chaptersProgress = nextProgress, status = nextStatus)
}

internal fun MangaListEntry.incrementedVolumes(splitCompleted: Boolean): MangaListEntry {
    val total = totalVolumes
    val nextProgress = if (total == null) volumesProgress + 1 else minOf(volumesProgress + 1, total)
    val nextStatus = if (total != null && nextProgress >= total) {
        format.completedMangaSection(splitCompleted)
    } else {
        status
    }
    return copy(volumesProgress = nextProgress, status = nextStatus)
}
