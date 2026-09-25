package com.luum.michi.app.mediaList

import com.luum.michi.app.core.model.MediaWorkStatus
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.storage.domain.SortScope
import com.luum.michi.app.mediaDetail.repository.media.MediaListEntryRepositoryImpl
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.isComplete
import com.luum.michi.app.mediaList.repository.anime.AnimeListRepositoryImpl
import com.luum.michi.app.mediaList.repository.manga.MangaListRepositoryImpl
import com.luum.michi.app.mediaList.ui.anime.state.AnimeListStateHolder
import com.luum.michi.app.mediaList.ui.manga.state.MangaListStateHolder
import com.luum.michi.app.root.hydrateSort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun animeCollection() = singleGroupCollection(
    listEntryJson(
        id = 1,
        status = "CURRENT",
        progress = 5,
        score = 8.0,
        media = animeMediaJson(
            id = 101,
            format = "TV",
            mediaStatus = "RELEASING",
            romaji = "Romaji Anime",
            english = "English Anime",
            native = "ネイティブ",
            episodes = 12,
            season = "WINTER",
            seasonYear = 2024,
        ),
    ),
    listEntryJson(
        id = 2,
        status = "COMPLETED",
        progress = 1,
        media = animeMediaJson(
            id = 102,
            format = "MOVIE",
            mediaStatus = "FINISHED",
            romaji = "Romaji Movie",
            english = null,
            episodes = 1,
            season = null,
            seasonYear = null,
        ),
    ),
)

private fun mangaCollection() = singleGroupCollection(
    listEntryJson(
        id = 10,
        status = "CURRENT",
        progress = 0,
        progressVolumes = 3,
        media = mangaMediaJson(
            id = 201,
            format = "NOVEL",
            mediaStatus = "RELEASING",
            romaji = "Romaji Novel",
            chapters = 0,
            volumes = 10,
        ),
    ),
    listEntryJson(
        id = 11,
        status = "CURRENT",
        progress = 42,
        media = mangaMediaJson(
            id = 202,
            format = "MANGA",
            mediaStatus = "FINISHED",
            romaji = "Romaji Manga",
            chapters = 100,
            volumes = 10,
        ),
    ),
)

private fun animeWired(graphQL: FakeGraphQL): Pair<AnimeListStateHolder, MediaListEntryRepositoryImpl> {
    val entryRepository = MediaListEntryRepositoryImpl(graphQL)
    val holder = AnimeListStateHolder(
        AnimeListRepositoryImpl(graphQL),
        entryRepository,
        CoroutineScope(Dispatchers.Unconfined),
    )
    return holder to entryRepository
}

private fun mangaWired(graphQL: FakeGraphQL): Pair<MangaListStateHolder, MediaListEntryRepositoryImpl> {
    val entryRepository = MediaListEntryRepositoryImpl(graphQL)
    val holder = MangaListStateHolder(
        MangaListRepositoryImpl(graphQL),
        entryRepository,
        CoroutineScope(Dispatchers.Unconfined),
    )
    return holder to entryRepository
}

class MediaListIntegrationTest {

    @Test
    fun animeMapsStatusTitlesWorkStatusAndSections() {
        val graphQL = FakeGraphQL(listOf(animeCollection()))
        val (holder) = animeWired(graphQL)

        holder.load(7)

        assertEquals(null, holder.error, "loader error: ${holder.error}")
        assertEquals(2, holder.entries.size, "raw entries")

        val entries = holder.entriesInSection(AnimeListSection.ALL)
        assertEquals(2, entries.size)
        val watching = entries.single { it.id == 101 }
        assertEquals(AnimeListSection.WATCHING, watching.status)
        // bestTitle prefers English over Romaji when both exist.
        assertEquals("English Anime", watching.title)
        assertEquals(listOf("Romaji Anime", "English Anime", "ネイティブ"), watching.titles)
        assertEquals(MediaWorkStatus.RELEASING, watching.mediaStatus)
        assertEquals(2024, watching.seasonYear)
        assertEquals(AnimeListSection.COMPLETED_MOVIE, entries.single { it.id == 102 }.status)
        assertEquals(MediaWorkStatus.FINISHED, entries.single { it.id == 102 }.mediaStatus)
        assertEquals(1, holder.countInSection(AnimeListSection.WATCHING))
        assertNull(holder.error)
    }

    @Test
    fun mangaNovelTracksVolumesWhileMangaTracksChapters() {
        val graphQL = FakeGraphQL(listOf(mangaCollection()))
        val (holder) = mangaWired(graphQL)

        holder.load(7)

        val entries = holder.entriesInSection(MangaListSection.ALL)
        assertEquals(2, entries.size)
        val novel = entries.single { it.id == 201 }
        assertTrue(novel.tracksByVolume)
        assertFalse(novel.isComplete())
        val manga = entries.single { it.id == 202 }
        assertFalse(manga.tracksByVolume)
        assertEquals(2, holder.countInSection(MangaListSection.CURRENT))
    }

    @Test
    fun incrementProgressAppliesOptimisticallyAndRollsBackOnFailure() {
        // Success path.
        val okGraphQL = FakeGraphQL(listOf(animeCollection(), emptyUpdateAck))
        val (okHolder) = animeWired(okGraphQL)
        okHolder.load(7)
        okHolder.incrementProgress(okHolder.entriesInSection(AnimeListSection.WATCHING).single())
        assertEquals(6, okHolder.entriesInSection(AnimeListSection.WATCHING).single().progress)
        assertEquals(2, okGraphQL.calls)

        // Failure path: the entry rolls back to the loaded value.
        val failGraphQL = FakeGraphQL(listOf(animeCollection(), emptyUpdateAck), failureAt = 1)
        val (failHolder) = animeWired(failGraphQL)
        failHolder.load(7)
        failHolder.incrementProgress(failHolder.entriesInSection(AnimeListSection.WATCHING).single())
        assertEquals(5, failHolder.entriesInSection(AnimeListSection.WATCHING).single().progress)
        assertEquals(2, failGraphQL.calls)
    }

    @Test
    fun saveProgressOmitsChaptersForNovels() {
        val graphQL = FakeGraphQL(listOf(mangaCollection(), emptyUpdateAck))
        val (holder) = mangaWired(graphQL)

        holder.load(7)
        val novel = holder.entriesInSection(MangaListSection.CURRENT).single { it.id == 201 }
        holder.incrementVolumes(novel)

        assertEquals(4, holder.entriesInSection(MangaListSection.CURRENT).single { it.id == 201 }.volumesProgress)
        val variables = graphQL.requests.last().variables
        assertEquals(4, variables?.get("progressVolumes")?.jsonPrimitive?.content?.toInt())
        assertNull(variables?.get("progress"))
    }

    @Test
    fun loaderErrorSurfacesWithEmptyEntries() {
        val graphQL = FakeGraphQL(listOf(animeCollection()), failureAt = 0)
        val (holder) = animeWired(graphQL)

        holder.load(7)

        assertTrue(holder.entriesInSection(AnimeListSection.ALL).isEmpty())
        assertIs<NetworkError.Http>(holder.error)
    }

    @Test
    fun filterByGenreAndFormatNarrowsSectionAndResets() {
        val graphQL = FakeGraphQL(listOf(singleGroupCollection(
            listEntryJson(
                id = 1,
                status = "CURRENT",
                progress = 3,
                media = animeMediaJson(
                    id = 101,
                    format = "TV",
                    romaji = "Action Show",
                    english = null,
                    genres = listOf("Action", "Adventure"),
                ),
            ),
            listEntryJson(
                id = 2,
                status = "CURRENT",
                progress = 1,
                media = animeMediaJson(
                    id = 102,
                    format = "MOVIE",
                    romaji = "Romance Film",
                    english = null,
                    genres = listOf("Romance"),
                ),
            ),
        )))
        val (holder) = animeWired(graphQL)

        holder.load(7)

        holder.updateListFilters(season = null, genres = listOf("Action"), formats = emptyList(), year = null)
        assertEquals(
            listOf(101),
            holder.entriesInSection(AnimeListSection.WATCHING).map { it.id },
        )

        holder.updateListFilters(season = null, genres = emptyList(), formats = listOf("MOVIE"), year = null)
        assertEquals(
            listOf(102),
            holder.entriesInSection(AnimeListSection.WATCHING).map { it.id },
        )

        holder.updateListFilters(season = null, genres = emptyList(), formats = emptyList(), year = null)
        assertEquals(2, holder.entriesInSection(AnimeListSection.WATCHING).size)
    }

    @Test
    fun sortByTitleRespectsOrder() {
        val graphQL = FakeGraphQL(listOf(singleGroupCollection(
            listEntryJson(
                id = 1,
                status = "CURRENT",
                progress = 3,
                media = animeMediaJson(id = 101, romaji = "Zulu", english = null),
            ),
            listEntryJson(
                id = 2,
                status = "CURRENT",
                progress = 1,
                media = animeMediaJson(id = 102, romaji = "Alpha", english = null),
            ),
        )))
        val (holder) = animeWired(graphQL)

        holder.load(7)

        holder.updateSort(UserListSort.TITLE, UserListOrder.ASCENDING, persist = false)
        assertEquals(
            listOf("Alpha", "Zulu"),
            holder.entriesInSection(AnimeListSection.WATCHING).map { it.title },
        )

        holder.updateSort(UserListSort.TITLE, UserListOrder.DESCENDING, persist = false)
        assertEquals(
            listOf("Zulu", "Alpha"),
            holder.entriesInSection(AnimeListSection.WATCHING).map { it.title },
        )
    }

    @Test
    fun updateSortPersistsPerScopeOnlyWhenAsked() {
        val persistence = MemorySortPersistence()
        val graphQL = FakeGraphQL(listOf(animeCollection()))
        val holder = AnimeListStateHolder(
            AnimeListRepositoryImpl(graphQL),
            MediaListEntryRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
            persistence,
        )

        holder.load(7)

        holder.updateSort(UserListSort.TITLE, UserListOrder.ASCENDING, persist = false)
        assertNull(persistence.loadSort(SortScope.ANIME))

        holder.updateSort(UserListSort.SCORE, UserListOrder.DESCENDING, persist = true)
        assertEquals("SCORE" to "DESCENDING", persistence.loadSort(SortScope.ANIME))
        assertNull(persistence.loadSort(SortScope.MANGA))
    }

    @Test
    fun hydrateSortRestoresKnownNamesAndFallsBackOnUnknown() {
        val persistence = MemorySortPersistence()
        persistence.saveSort(SortScope.ANIME, "TITLE", "ASCENDING")

        assertEquals(
            UserListSort.TITLE to UserListOrder.ASCENDING,
            hydrateSort(persistence, SortScope.ANIME),
        )
        assertNull(hydrateSort(persistence, SortScope.MANGA))

        persistence.saveSort(SortScope.MANGA, "NOPE", "ASCENDING")
        assertNull(hydrateSort(persistence, SortScope.MANGA))
    }

    @Test
    fun splitCompletedMapsMergedOrPerFormat() {
        val graphQL = FakeGraphQL(listOf(animeCollection(), animeCollection()))
        val (holder) = animeWired(graphQL)

        holder.load(7)
        assertEquals(1, holder.entriesInSection(AnimeListSection.COMPLETED_MOVIE).size)
        assertEquals(0, holder.entriesInSection(AnimeListSection.COMPLETED).size)

        // Flipping split bypasses the TTL: same server data, fresh mapping.
        holder.load(7, splitCompleted = false)
        assertEquals(1, holder.entriesInSection(AnimeListSection.COMPLETED).size)
        assertEquals(0, holder.entriesInSection(AnimeListSection.COMPLETED_MOVIE).size)
    }

    @Test
    fun mangaSplitCompletedMapsMergedOrPerFormat() {
        fun completedCollection() = singleGroupCollection(
            listEntryJson(
                id = 10,
                status = "COMPLETED",
                progress = 10,
                progressVolumes = 10,
                media = mangaMediaJson(
                    id = 201,
                    format = "NOVEL",
                    romaji = "Novel Done",
                    chapters = 0,
                    volumes = 10,
                ),
            ),
            listEntryJson(
                id = 11,
                status = "COMPLETED",
                progress = 100,
                media = mangaMediaJson(
                    id = 202,
                    format = "MANGA",
                    romaji = "Manga Done",
                    chapters = 100,
                    volumes = 10,
                ),
            ),
        )
        val graphQL = FakeGraphQL(listOf(completedCollection(), completedCollection()))
        val (holder) = mangaWired(graphQL)

        holder.load(7)
        assertEquals(1, holder.entriesInSection(MangaListSection.COMPLETED_NOVEL).size)
        assertEquals(1, holder.entriesInSection(MangaListSection.COMPLETED_MANGA).size)
        assertEquals(0, holder.entriesInSection(MangaListSection.COMPLETED).size)

        // Flipping split bypasses the TTL: same server data, fresh mapping.
        holder.load(7, splitCompleted = false)
        assertEquals(2, holder.entriesInSection(MangaListSection.COMPLETED).size)
        assertEquals(0, holder.entriesInSection(MangaListSection.COMPLETED_NOVEL).size)
    }

    @Test
    fun adultEntriesHideWhenSettingOff() {
        fun mixedCollection() = singleGroupCollection(
            listEntryJson(
                id = 1,
                status = "CURRENT",
                progress = 3,
                media = animeMediaJson(id = 101, romaji = "Clean Show", english = null),
            ),
            listEntryJson(
                id = 2,
                status = "CURRENT",
                progress = 1,
                media = animeMediaJson(id = 102, romaji = "Adult Show", english = null, isAdult = true),
            ),
        )
        val graphQL = FakeGraphQL(listOf(mixedCollection()))
        val (holder) = animeWired(graphQL)

        holder.load(7)

        assertEquals(2, holder.entriesInSection(AnimeListSection.WATCHING).size)
        assertEquals(
            listOf(101),
            holder.entriesInSection(AnimeListSection.WATCHING, hideAdult = true).map { it.id },
        )
        // Rail counts stay raw by design (loaded list, never view state).
        assertEquals(2, holder.countInSection(AnimeListSection.WATCHING))
    }

    @Test
    fun decodeRealShapedCollectionMapsIsAdult() {
        // Wire-shaped payload (keys as AniList delivers them, plus an
        // unknown field): guards against doc drift on field naming, through
        // the real DTO decode + mapper + holder.
        val wireJson = """
            {
              "MediaListCollection": {
                "lists": [
                  {
                    "name": "Watching",
                    "status": "CURRENT",
                    "isCustomList": false,
                    "entries": [
                      {
                        "id": 1,
                        "status": "CURRENT",
                        "progress": 3,
                        "score": 0,
                        "media": {
                          "id": 101,
                          "title": { "romaji": "Clean Show" },
                          "format": "TV",
                          "isAdult": false,
                          "__debug": 1
                        }
                      },
                      {
                        "id": 2,
                        "status": "CURRENT",
                        "progress": 1,
                        "score": 0,
                        "media": {
                          "id": 102,
                          "title": { "romaji": "Adult Show" },
                          "format": "TV",
                          "isAdult": true
                        }
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
        val graphQL = FakeGraphQL(listOf(Json.parseToJsonElement(wireJson)))
        val (holder) = animeWired(graphQL)

        holder.load(7)

        val entries = holder.entriesInSection(AnimeListSection.WATCHING)
        assertEquals(false, entries.single { it.id == 101 }.isAdult)
        assertEquals(true, entries.single { it.id == 102 }.isAdult)
        assertEquals(
            listOf(101),
            holder.entriesInSection(AnimeListSection.WATCHING, hideAdult = true).map { it.id },
        )
    }
}
