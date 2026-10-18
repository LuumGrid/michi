package com.luum.michi.app.mediaList

import com.luum.michi.app.core.media.domain.MediaFormat
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.completesOnIncrement
import com.luum.michi.app.mediaList.domain.anime.model.incremented
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.completesOnChapterIncrement
import com.luum.michi.app.mediaList.domain.manga.model.completesOnVolumeIncrement
import com.luum.michi.app.mediaList.domain.manga.model.incrementedChapters
import com.luum.michi.app.mediaList.domain.manga.model.incrementedVolumes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The `completesOn*` predicates decide when the list hands the `+1` to the
 * editor instead of saving it. Each one must agree exactly with the completion
 * rule its `incremented*` twin already applies, so the two never drift apart.
 * Entries sit in the sections that actually show the button (WATCHING/CURRENT).
 */
class MediaListCompletionTest {

    // ── Anime ──

    @Test
    fun animeUnknownTotalNeverCompletes() {
        assertFalse(animeEntry(progress = 5, totalEpisodes = null).completesOnIncrement())
    }

    @Test
    fun animeIncrementBelowTheTotalDoesNotComplete() {
        assertFalse(animeEntry(progress = 5, totalEpisodes = 12).completesOnIncrement())
    }

    @Test
    fun animeIncrementOnTheTotalCompletes() {
        assertTrue(animeEntry(progress = 11, totalEpisodes = 12).completesOnIncrement())
    }

    @Test
    fun animeCompletingPredicateMatchesIncremented() {
        val entries = listOf(
            animeEntry(progress = 0, totalEpisodes = null),
            animeEntry(progress = 5, totalEpisodes = 12),
            animeEntry(progress = 11, totalEpisodes = 12),
            animeEntry(progress = 0, totalEpisodes = 1),
            animeEntry(progress = 12, totalEpisodes = 12),
        )
        for (entry in entries) {
            assertCompletionMatches(
                expected = entry.completesOnIncrement(),
                completed = entry.incremented(splitCompleted = false).status == AnimeListSection.COMPLETED,
                entry = entry,
            )
        }
    }

    // ── Manga chapters ──

    @Test
    fun mangaChaptersUnknownTotalNeverCompletes() {
        assertFalse(mangaEntry(chaptersProgress = 5, totalChapters = null).completesOnChapterIncrement())
    }

    @Test
    fun mangaChaptersBelowTheTotalDoesNotComplete() {
        assertFalse(mangaEntry(chaptersProgress = 5, totalChapters = 100).completesOnChapterIncrement())
    }

    @Test
    fun mangaChaptersOnTheTotalCompletes() {
        assertTrue(mangaEntry(chaptersProgress = 99, totalChapters = 100).completesOnChapterIncrement())
    }

    @Test
    fun mangaChapterPredicateMatchesIncrementedChapters() {
        val entries = listOf(
            mangaEntry(chaptersProgress = 0, totalChapters = null),
            mangaEntry(chaptersProgress = 42, totalChapters = 100),
            mangaEntry(chaptersProgress = 99, totalChapters = 100),
            mangaEntry(chaptersProgress = 0, totalChapters = 1),
            mangaEntry(chaptersProgress = 100, totalChapters = 100),
        )
        for (entry in entries) {
            assertCompletionMatches(
                expected = entry.completesOnChapterIncrement(),
                completed = entry.incrementedChapters(splitCompleted = false).status == MangaListSection.COMPLETED,
                entry = entry,
            )
        }
    }

    // ── Manga volumes ──

    @Test
    fun mangaVolumesUnknownTotalNeverCompletes() {
        assertFalse(mangaEntry(volumesProgress = 3, totalVolumes = null).completesOnVolumeIncrement())
    }

    @Test
    fun mangaVolumesBelowTheTotalDoesNotComplete() {
        assertFalse(mangaEntry(volumesProgress = 3, totalVolumes = 10).completesOnVolumeIncrement())
    }

    @Test
    fun mangaVolumesOnTheTotalCompletesEvenWithoutChapters() {
        // A novel tracks volumes only: no chapter total must still complete.
        val novel = mangaEntry(
            chaptersProgress = 0,
            totalChapters = null,
            volumesProgress = 9,
            totalVolumes = 10,
        )
        assertTrue(novel.completesOnVolumeIncrement())
    }

    @Test
    fun mangaVolumePredicateMatchesIncrementedVolumes() {
        val entries = listOf(
            mangaEntry(volumesProgress = 3, totalVolumes = null),
            mangaEntry(volumesProgress = 3, totalVolumes = 10),
            mangaEntry(volumesProgress = 9, totalVolumes = 10),
            mangaEntry(chaptersProgress = 0, totalChapters = null, volumesProgress = 9, totalVolumes = 10),
        )
        for (entry in entries) {
            assertCompletionMatches(
                expected = entry.completesOnVolumeIncrement(),
                completed = entry.incrementedVolumes(splitCompleted = false).status == MangaListSection.COMPLETED,
                entry = entry,
            )
        }
    }

    private fun assertCompletionMatches(expected: Boolean, completed: Boolean, entry: Any) {
        assertEquals(expected, completed, "predicate and incremented disagree for $entry")
    }
}

private fun animeEntry(
    progress: Int,
    totalEpisodes: Int?,
    status: AnimeListSection = AnimeListSection.WATCHING,
): AnimeListEntry = AnimeListEntry(
    id = 1,
    title = "Anime",
    format = MediaFormat.TV,
    status = status,
    progress = progress,
    totalEpisodes = totalEpisodes,
    score = 0.0,
    nextEpisodeRelease = null,
    paletteHex = null,
)

private fun mangaEntry(
    chaptersProgress: Int = 0,
    totalChapters: Int? = null,
    volumesProgress: Int = 0,
    totalVolumes: Int? = null,
    status: MangaListSection = MangaListSection.CURRENT,
): MangaListEntry = MangaListEntry(
    id = 1,
    title = "Manga",
    format = MediaFormat.MANGA,
    status = status,
    chaptersProgress = chaptersProgress,
    totalChapters = totalChapters,
    volumesProgress = volumesProgress,
    totalVolumes = totalVolumes,
    score = 0.0,
    paletteHex = null,
)
