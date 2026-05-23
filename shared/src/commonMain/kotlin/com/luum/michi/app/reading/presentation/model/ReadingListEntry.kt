package com.luum.michi.app.reading.presentation.model

import androidx.compose.ui.graphics.Color
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.model.MediaReleaseDateTime

internal data class ReadingListEntry(
    val id: Int,
    val title: String,
    val format: String,
    val status: ReadingListSection,
    val chaptersProgress: Int,
    val totalChapters: Int?,
    val volumesProgress: Int,
    val totalVolumes: Int?,
    val score: String,
    val nextChapterRelease: MediaReleaseDateTime?,
    val palette: List<Color>,
)

internal fun ReadingListEntry.chaptersProgressLabel(): String {
    val total = totalChapters?.toString() ?: "?"
    return "$chaptersProgress / $total"
}

internal fun ReadingListEntry.volumesProgressLabel(): String {
    val total = totalVolumes?.toString() ?: "?"
    return "$volumesProgress / $total"
}

internal fun ReadingListEntry.chaptersProgressRatio(): Float {
    val total = totalChapters ?: return 0f
    return if (total == 0) 0f else (chaptersProgress.toFloat() / total).coerceIn(0f, 1f)
}

internal fun ReadingListEntry.canIncrementChapters(): Boolean {
    val total = totalChapters
    return total == null || chaptersProgress < total
}

internal fun ReadingListEntry.canIncrementVolumes(): Boolean {
    val total = totalVolumes
    return total == null || volumesProgress < total
}

internal fun ReadingListEntry.releaseLabel(strings: LanguageStrings): String? {
    return nextChapterRelease?.let { release ->
        strings.nextChapterReleaseLabel(chapterNumber = chaptersProgress + 1, releaseDateTime = release)
    }
}

internal fun ReadingListEntry.behindLabel(strings: LanguageStrings): String? {
    val total = totalChapters
    val behind = if (total != null) total - chaptersProgress else 0
    return if (status == ReadingListSection.READING && behind > 0) {
        strings.chaptersBehind(behind)
    } else {
        null
    }
}

internal fun ReadingListEntry.incrementedChapters(): ReadingListEntry {
    val total = totalChapters
    val nextProgress = if (total == null) chaptersProgress + 1 else minOf(chaptersProgress + 1, total)
    val nextStatus = if (total != null && nextProgress >= total) ReadingListSection.COMPLETED else status
    return copy(chaptersProgress = nextProgress, status = nextStatus)
}

internal fun ReadingListEntry.incrementedVolumes(): ReadingListEntry {
    val total = totalVolumes
    val nextProgress = if (total == null) volumesProgress + 1 else minOf(volumesProgress + 1, total)
    return copy(volumesProgress = nextProgress)
}
