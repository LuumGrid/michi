package com.luum.michi.app.reading.presentation.sample

import androidx.compose.ui.graphics.Color
import com.luum.michi.app.core.model.MediaReleaseDateTime
import com.luum.michi.app.reading.presentation.model.ReadingListEntry
import com.luum.michi.app.reading.presentation.model.ReadingListSection

internal val ReadingSampleEntries: List<ReadingListEntry> = listOf(
    ReadingListEntry(
        id = 1,
        title = "Berserk",
        format = "Manga | Dark Fantasy",
        status = ReadingListSection.READING,
        chaptersProgress = 375,
        totalChapters = null,
        volumesProgress = 42,
        totalVolumes = null,
        score = "9.5",
        nextChapterRelease = MediaReleaseDateTime(day = 5, month = 6, year = 2026, hour = 0, minute = 0),
        palette = listOf(Color(0xFF1E293B), Color(0xFF64748B)),
    ),
    ReadingListEntry(
        id = 2,
        title = "Monster",
        format = "Manga | Thriller",
        status = ReadingListSection.COMPLETED,
        chaptersProgress = 162,
        totalChapters = 162,
        volumesProgress = 18,
        totalVolumes = 18,
        score = "9.2",
        nextChapterRelease = null,
        palette = listOf(Color(0xFF450A0A), Color(0xFF991B1B)),
    ),
    ReadingListEntry(
        id = 3,
        title = "Vagabond",
        format = "Manga | Historical",
        status = ReadingListSection.PAUSED,
        chaptersProgress = 327,
        totalChapters = 327,
        volumesProgress = 37,
        totalVolumes = 37,
        score = "9.3",
        nextChapterRelease = null,
        palette = listOf(Color(0xFF064E3B), Color(0xFF059669)),
    ),
    ReadingListEntry(
        id = 4,
        title = "Chainsaw Man",
        format = "Manga | Action",
        status = ReadingListSection.READING,
        chaptersProgress = 165,
        totalChapters = null,
        volumesProgress = 17,
        totalVolumes = null,
        score = "8.7",
        nextChapterRelease = MediaReleaseDateTime(day = 23, month = 5, year = 2026, hour = 12, minute = 0),
        palette = listOf(Color(0xFF7C2D12), Color(0xFFEA580C)),
    ),
)
