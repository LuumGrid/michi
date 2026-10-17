package com.luum.michi.app.mediaList

import com.luum.michi.app.DeferredDispatcher
import com.luum.michi.app.FakeGraphQL
import com.luum.michi.app.core.favourite.repository.FavouriteRepositoryImpl
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.medialist.domain.MediaListViewerEntry
import com.luum.michi.app.core.media.domain.MediaFormat
import com.luum.michi.app.core.media.domain.MediaWorkStatus
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.medialist.domain.SortScope
import com.luum.michi.app.core.medialist.repository.MediaListEntryRepositoryImpl
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.incremented
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.incrementedChapters
import com.luum.michi.app.mediaList.domain.manga.model.isComplete
import com.luum.michi.app.mediaList.repository.anime.AnimeListRepositoryImpl
import com.luum.michi.app.mediaList.repository.manga.MangaListRepositoryImpl
import com.luum.michi.app.mediaList.ui.anime.state.AnimeListStateHolder
import com.luum.michi.app.mediaList.ui.manga.state.MangaListStateHolder
import com.luum.michi.app.root.hydrateSort
import com.luum.michi.app.root.resolveInitialSort
import com.luum.michi.app.settings.domain.model.ListSort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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
    val entryRepository = MediaListEntryRepositoryImpl(graphQL, FavouriteRepositoryImpl(graphQL))
    val holder = AnimeListStateHolder(
        AnimeListRepositoryImpl(graphQL),
        entryRepository,
        CoroutineScope(Dispatchers.Unconfined),
    )
    return holder to entryRepository
}

private fun mangaWired(graphQL: FakeGraphQL): Pair<MangaListStateHolder, MediaListEntryRepositoryImpl> {
    val entryRepository = MediaListEntryRepositoryImpl(graphQL, FavouriteRepositoryImpl(graphQL))
    val holder = MangaListStateHolder(
        MangaListRepositoryImpl(graphQL),
        entryRepository,
        CoroutineScope(Dispatchers.Unconfined),
    )
    return holder to entryRepository
}

/** Entry repo whose progress save is held at [gate], to queue two `+1`s. */
private class GatedEntryRepository(
    private val failAtCall: Int = -1,
) : MediaListEntryRepository {
    val gate = CompletableDeferred<Unit>()
    var progressCalls = 0
        private set

    override suspend fun saveEntry(
        mediaId: Int,
        status: MediaListStatus,
        progress: Int,
        progressVolumes: Int?,
        score: Float,
        advancedScores: List<Float>?,
        notes: String,
        repeat: Int,
        priority: Int,
        isPrivate: Boolean,
        hiddenFromStatusLists: Boolean,
        startedAtMillis: Long?,
        completedAtMillis: Long?,
    ): NetworkResult<MediaListViewerEntry> = NetworkResult.Failure(NetworkError.Unknown(null))

    override suspend fun saveProgress(
        mediaId: Int,
        progress: Int?,
        status: MediaListStatus?,
        progressVolumes: Int?,
    ): NetworkResult<Unit> {
        progressCalls++
        gate.await()
        return if (progressCalls == failAtCall) {
            NetworkResult.Failure(NetworkError.Http(500, null))
        } else {
            NetworkResult.Success(Unit)
        }
    }

    override suspend fun toggleFavourite(mediaId: Int, isManga: Boolean): NetworkResult<Unit> =
        NetworkResult.Success(Unit)

    override suspend fun deleteEntry(entryId: Int): NetworkResult<Unit> = NetworkResult.Success(Unit)
}

class MediaListIntegrationTest {
    @Test
    fun openingTheListFetchesWithoutACache() {
        val graphQL = FakeGraphQL(listOf(animeCollection()))
        val (holder) = animeWired(graphQL)

        holder.load(7) // non-forced, nothing cached yet

        assertEquals(1, graphQL.calls, "a non-forced load with no cache still fetches")
        assertTrue(holder.entries.isNotEmpty())
    }

    @Test
    fun twoQuickTapsWithAFailureRevertToTheConfirmedProgress() {
        val graphQL = FakeGraphQL(listOf(animeCollection()))
        val dispatcher = DeferredDispatcher()
        // A (call 1) succeeds; B (call 2) is scripted to fail — but with the fix
        // B has nothing new to send, so it skips and the value stays confirmed.
        val entries = GatedEntryRepository(failAtCall = 2)
        val holder = AnimeListStateHolder(
            AnimeListRepositoryImpl(graphQL),
            entries,
            CoroutineScope(dispatcher),
        )
        holder.load(7)
        dispatcher.runAll()
        val entry = holder.entries.first { it.id == 101 }
        assertEquals(5, entry.progress)

        holder.incrementProgress(entry) // tap A
        holder.incrementProgress(entry) // tap B
        dispatcher.runAll() // A coalesces the latest value (7) and holds at the gate
        assertEquals(7, holder.entries.first { it.id == 101 }.progress)

        entries.gate.complete(Unit)
        dispatcher.runAll() // A succeeds (confirmed = 7); B sees nothing new and skips

        // Never reverts to the stale tap-time copy (5): the server has 7.
        assertEquals(7, holder.entries.first { it.id == 101 }.progress, "kept the confirmed value")
        assertEquals(1, entries.progressCalls, "B did not re-send")
    }

    @Test
    fun aSecondTapJustAsTheFirstSaveEndsIsNotLost() {
        val graphQL = FakeGraphQL(listOf(animeCollection()))
        val dispatcher = DeferredDispatcher()
        val entries = GatedEntryRepository()
        val holder = AnimeListStateHolder(
            AnimeListRepositoryImpl(graphQL),
            entries,
            CoroutineScope(dispatcher),
        )
        holder.load(7)
        dispatcher.runAll()
        assertEquals(5, holder.entries.first { it.id == 101 }.progress)

        holder.incrementProgress(holder.entries.first { it.id == 101 }) // tap A
        dispatcher.runAll() // A captures sent = 6 and holds at the gate
        entries.gate.complete(Unit) // the first save just finished
        // Tap B lands now, before A's cleanup runs. A's cleanup must not drop
        // the confirmed base and swallow B's +1.
        holder.incrementProgress(holder.entries.first { it.id == 101 })
        dispatcher.runAll()

        assertEquals(2, entries.progressCalls, "both taps are saved")
        assertEquals(7, holder.entries.first { it.id == 101 }.progress)
    }

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
    fun aTtlSkippedLoadKeepsTheProgressActionError() {
        // One collection page only: a second fetch would blow up, so passing
        // proves the second load() never hit the network.
        val graphQL = FakeGraphQL(listOf(animeCollection(), emptyUpdateAck), failureAt = 1)
        val (holder) = animeWired(graphQL)

        holder.load(7)
        val watching = holder.entriesInSection(AnimeListSection.WATCHING).single()
        holder.incrementProgress(watching) // the +1 save fails
        assertEquals(2, graphQL.calls)
        assertIs<NetworkError.Http>(holder.actionError)
        assertEquals(5, holder.entriesInSection(AnimeListSection.WATCHING).single().progress)

        // Root calls load() on every tab switch; within the TTL nothing is
        // replaced, so the +1 error and rollback state must survive.
        holder.load(7)

        assertEquals(2, graphQL.calls, "a TTL-skipped load must not fetch")
        assertIs<NetworkError.Http>(
            holder.actionError,
            "a TTL-skipped load must not clear the +1 error (nor its rollback state)",
        )
        assertEquals(5, holder.entriesInSection(AnimeListSection.WATCHING).single().progress)
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
            MediaListEntryRepositoryImpl(graphQL, FavouriteRepositoryImpl(graphQL)),
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
    fun resolveInitialSortPrefersSavedThenDefault() {
        val saved = UserListSort.SCORE to UserListOrder.ASCENDING
        val restored = resolveInitialSort(saved, ListSort.TITLE)
        assertEquals(UserListSort.SCORE, restored.option)
        assertEquals(UserListOrder.ASCENDING, restored.order)
        assertTrue(restored.persist)

        val defaulted = resolveInitialSort(null, ListSort.TITLE)
        assertEquals(UserListSort.TITLE, defaulted.option)
        assertEquals(UserListOrder.ASCENDING, defaulted.order)
        assertFalse(defaulted.persist)
    }

    @Test
    fun resolveInitialSortMapsEveryDefaultSort() {
        fun mapped(sort: ListSort) = resolveInitialSort(null, sort).let { it.option to it.order }
        assertEquals(UserListSort.TITLE to UserListOrder.ASCENDING, mapped(ListSort.TITLE))
        assertEquals(UserListSort.SCORE to UserListOrder.DESCENDING, mapped(ListSort.SCORE))
        assertEquals(UserListSort.LAST_UPDATED to UserListOrder.DESCENDING, mapped(ListSort.UPDATED))
        assertEquals(UserListSort.LAST_ADDED to UserListOrder.DESCENDING, mapped(ListSort.ADDED))
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
    fun incrementingToTheLastEpisodeHonorsTheSplitFlag() {
        val movie = AnimeListEntry(
            id = 1,
            title = "M",
            format = MediaFormat.MOVIE,
            status = AnimeListSection.WATCHING,
            progress = 0,
            totalEpisodes = 1,
            score = 0.0,
            nextEpisodeRelease = null,
            paletteHex = null,
        )

        assertEquals(AnimeListSection.COMPLETED_MOVIE, movie.incremented(splitCompleted = true).status)
        assertEquals(AnimeListSection.COMPLETED, movie.incremented(splitCompleted = false).status)
    }

    @Test
    fun incrementingMangaToTheEndUsesTheFormatSectionWhenSplit() {
        val unknownFormat = MangaListEntry(
            id = 1,
            title = "M",
            format = MediaFormat.UNKNOWN,
            status = MangaListSection.CURRENT,
            chaptersProgress = 0,
            totalChapters = 1,
            volumesProgress = 0,
            totalVolumes = null,
            score = 0.0,
            paletteHex = null,
        )

        // Unknown format falls into the generic manga section (like anime's TV),
        // never the merged COMPLETED that the split section list does not show.
        assertEquals(MangaListSection.COMPLETED_MANGA, unknownFormat.incrementedChapters(splitCompleted = true).status)
        assertEquals(MangaListSection.COMPLETED, unknownFormat.incrementedChapters(splitCompleted = false).status)
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

/** Entry with only the fields the loader generation tests need. */
private fun lateAnimeEntry(id: Int) = AnimeListEntry(
    id = id,
    title = "Media $id",
    format = MediaFormat.TV,
    status = AnimeListSection.WATCHING,
    progress = 0,
    totalEpisodes = 12,
    score = 0.0,
    nextEpisodeRelease = null,
    paletteHex = null,
)

/**
 * Repo whose fetch ignores cancellation and answers only when its own gate
 * completes: a load cancelled by a forced reload still tries to write, which the
 * MediaListLoader generation guard must drop.
 */
private class LateAnimeRepository : AnimeListRepository {
    val gateA = CompletableDeferred<Unit>()
    val gateB = CompletableDeferred<Unit>()
    var failA = false
    private var calls = 0

    override suspend fun loadList(userId: Int, splitCompleted: Boolean): NetworkResult<List<AnimeListEntry>> {
        val call = ++calls
        val gate = if (call == 1) gateA else gateB
        // Non-cancellable wait: cancelling the coroutine leaves it suspended
        // until the gate completes (a fetch that ignores cancellation).
        suspendCoroutine<Unit> { cont -> gate.invokeOnCompletion { cont.resume(Unit) } }
        return when {
            call == 1 && failA -> NetworkResult.Failure(NetworkError.Http(500, null))
            call == 1 -> NetworkResult.Success(listOf(lateAnimeEntry(101)))
            else -> NetworkResult.Success(listOf(lateAnimeEntry(102)))
        }
    }
}

private fun lateWired(repository: AnimeListRepository) = AnimeListStateHolder(
    repository = repository,
    entryRepository = GatedEntryRepository(),
    scope = CoroutineScope(Dispatchers.Unconfined),
)

/**
 * The loader's generation guard: a load cancelled by a forced reload must not
 * write its entries or its error even when its fetch ignores cancellation and
 * answers late.
 */
class MediaListLoaderGenerationTest {

    @Test
    fun aCancelledLoadNeverOverwritesTheForcedLoadsEntries() {
        val repository = LateAnimeRepository()
        val holder = lateWired(repository)

        holder.load(7) // A, held at gateA
        holder.load(7, forceRefresh = true) // B forced, cancels A

        repository.gateB.complete(Unit) // B lands first
        repository.gateA.complete(Unit) // A's late response lands afterwards

        assertEquals(listOf(102), holder.entries.map { it.id }, "A's entries must not win")
        assertNull(holder.error)
    }

    @Test
    fun aCancelledLoadNeverOverwritesTheForcedLoadsError() {
        val repository = LateAnimeRepository().apply { failA = true }
        val holder = lateWired(repository)

        holder.load(7) // A (fails late), held at gateA
        holder.load(7, forceRefresh = true) // B forced

        repository.gateB.complete(Unit) // B succeeds
        repository.gateA.complete(Unit) // A's late failure lands afterwards

        assertNull(holder.error, "A's error must not win")
        assertEquals(listOf(102), holder.entries.map { it.id })
    }
}
