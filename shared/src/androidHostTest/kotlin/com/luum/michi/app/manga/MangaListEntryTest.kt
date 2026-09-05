package com.luum.michi.app.manga

import com.luum.michi.app.manga.domain.model.MangaListEntry
import com.luum.michi.app.manga.domain.model.MangaListSection
import com.luum.michi.app.manga.domain.model.MangaMediaFormat
import com.luum.michi.app.manga.domain.model.canIncrementChapters
import com.luum.michi.app.manga.domain.model.canIncrementVolumes
import com.luum.michi.app.manga.domain.model.formattedScore
import com.luum.michi.app.manga.domain.model.incrementedChapters
import com.luum.michi.app.manga.domain.model.incrementedVolumes
import com.luum.michi.app.manga.domain.model.isComplete
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun novelEntry(volumesProgress: Int) = MangaListEntry(
    id = 1,
    title = "Novel",
    format = MangaMediaFormat.NOVEL,
    status = MangaListSection.CURRENT,
    chaptersProgress = 0,
    totalChapters = 0,
    volumesProgress = volumesProgress,
    totalVolumes = 3,
    tracksByVolume = true,
    score = 0.0,
    nextChapterRelease = null,
    palette = emptyList(),
)

private fun mangaEntry(chaptersProgress: Int) = MangaListEntry(
    id = 2,
    title = "Manga",
    format = MangaMediaFormat.MANGA,
    status = MangaListSection.CURRENT,
    chaptersProgress = chaptersProgress,
    totalChapters = 8,
    volumesProgress = 0,
    totalVolumes = 2,
    tracksByVolume = false,
    score = 8.5,
    nextChapterRelease = null,
    palette = emptyList(),
)

class MangaListEntryTest {

    @Test
    fun novelIsNotCompleteBeforeReadingAnyVolume() {
        val entry = novelEntry(volumesProgress = 0)
        assertFalse(entry.isComplete())
        assertTrue(entry.canIncrementVolumes())
    }

    @Test
    fun novelCompletesWhenVolumesReachTotal() {
        val entry = novelEntry(volumesProgress = 2).incrementedVolumes()
        assertEquals(3, entry.volumesProgress)
        assertEquals(MangaListSection.COMPLETED, entry.status)
        assertTrue(entry.isComplete())
    }

    @Test
    fun novelVolumesClampAtTotal() {
        val entry = novelEntry(volumesProgress = 3).incrementedVolumes()
        assertEquals(3, entry.volumesProgress)
        assertEquals(MangaListSection.COMPLETED, entry.status)
    }

    @Test
    fun chapterEntryStillCompletesByChapters() {
        val entry = mangaEntry(chaptersProgress = 7).incrementedChapters()
        assertEquals(8, entry.chaptersProgress)
        assertEquals(MangaListSection.COMPLETED, entry.status)
        assertTrue(entry.isComplete())
    }

    @Test
    fun chapterEntryIsNotCompleteMidway() {
        val entry = mangaEntry(chaptersProgress = 3)
        assertFalse(entry.isComplete())
        assertTrue(entry.canIncrementChapters())
    }

    @Test
    fun formattedScoreMatchesPreviousDisplay() {
        assertEquals("-", novelEntry(0).formattedScore())
        assertEquals("8.5", mangaEntry(0).formattedScore())
        assertEquals("8", mangaEntry(0).copy(score = 8.0).formattedScore())
    }
}
