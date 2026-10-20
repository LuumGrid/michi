package com.luum.michi.app.discover

import com.luum.michi.app.DeferredDispatcher
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.model.ExploreResult
import com.luum.michi.app.discover.domain.model.MediaCatalogFilter
import com.luum.michi.app.discover.domain.model.genreCatalogFilter
import com.luum.michi.app.discover.ui.catalog.state.CatalogGridStateHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun gridItem(id: Int) = MediaGridItem(
    mediaId = id,
    title = "Media $id",
    cover = null,
    paletteHex = null,
    format = "TV",
    year = 2020,
    averageScore = null,
    favourites = 42,
    genres = listOf("Action", "Comedy"),
)

internal fun gridPage(items: List<Int>, hasNextPage: Boolean = false, currentPage: Int = 1) =
    Page(items.map { gridItem(it) }, hasNextPage = hasNextPage, currentPage = currentPage)

private fun genreHolder(
    repository: ExploreRepository,
    hideAdult: Boolean = true,
) = CatalogGridStateHolder(
    repository = repository,
    scope = CoroutineScope(Dispatchers.Unconfined),
    hideAdult = hideAdult,
)

/**
 * The catalog grid only uses [ExploreRepository.searchMediaGrid]; the rest of
 * the interface is stubbed so the double stays a full implementation.
 */
internal open class FakeCatalogExploreRepository : ExploreRepository {
    override suspend fun searchMedia(
        type: MediaType,
        query: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
        sort: String,
        page: Int,
        perPage: Int,
        season: String?,
        onList: Boolean?,
        hideAdult: Boolean,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> = NetworkResult.Success(Page(emptyList(), false, 1))

    override suspend fun searchMediaGrid(
        filter: MediaCatalogFilter,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> = NetworkResult.Success(Page(emptyList(), false, page))

    override suspend fun searchCharacters(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> = NetworkResult.Success(Page(emptyList(), false, 1))

    override suspend fun searchStaff(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> = NetworkResult.Success(Page(emptyList(), false, 1))

    override suspend fun searchStudios(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> = NetworkResult.Success(Page(emptyList(), false, 1))
}

/**
 * Repository double: canned pages per page number plus call counters and the
 * last request, to prove cache hits, the adult gate and that a filter/sort
 * change re-queries.
 */
internal class RecordingCatalogRepository(
    private val pages: Map<Int, Page<MediaGridItem>> = emptyMap(),
) : FakeCatalogExploreRepository() {
    var calls = 0
        private set
    var lastSort: String? = null
        private set
    var lastFilter: MediaCatalogFilter? = null
        private set
    var lastPage: Int? = null
        private set
    var lastHideAdult: Boolean? = null
        private set

    override suspend fun searchMediaGrid(
        filter: MediaCatalogFilter,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> {
        calls++
        lastSort = sort
        lastFilter = filter
        lastPage = page
        lastHideAdult = hideAdult
        return NetworkResult.Success(pages[page] ?: Page(emptyList(), false, page))
    }
}

class GenreDetailIntegrationTest {

    @Test
    fun loadHydratesPageOneByPopularity() {
        val repository = RecordingCatalogRepository(
            pages = mapOf(1 to gridPage(listOf(7), hasNextPage = true)),
        )
        val holder = genreHolder(repository)

        holder.load(genreCatalogFilter("Action", MediaType.ANIME))

        assertEquals(MediaType.ANIME, holder.filter.type)
        assertEquals(listOf("Action"), holder.filter.genres)
        assertEquals(listOf(7), holder.media.items.map { it.mediaId })
        val item = holder.media.items.single()
        assertEquals(42, item.favourites)
        assertEquals(listOf("Action", "Comedy"), item.genres)
        assertTrue(holder.pageLoaded)
        assertFalse(holder.isLoading)
        assertNull(holder.error)
        assertEquals(1, repository.calls)
        // Default selection: a genre opens ordered by popularity, descending.
        assertEquals(UserListSort.POPULARITY, holder.sortOption)
        assertEquals(UserListOrder.DESCENDING, holder.sortOrder)
        assertEquals("POPULARITY_DESC", repository.lastSort)
        assertEquals(MediaType.ANIME, repository.lastFilter?.type)
        assertEquals(listOf("Action"), repository.lastFilter?.genres, "the genre travels as genre_in")
        assertEquals(true, repository.lastHideAdult)
    }

    @Test
    fun cacheHitSkipsNetworkWhenTheSameGenreReopens() {
        val repository = RecordingCatalogRepository()
        val holder = genreHolder(repository)

        holder.load(genreCatalogFilter("Action", MediaType.ANIME))
        holder.load(genreCatalogFilter("Comedy", MediaType.ANIME))
        holder.load(genreCatalogFilter("Action", MediaType.ANIME))

        assertEquals(2, repository.calls)
        assertEquals(listOf("Action"), holder.filter.genres)
    }

    @Test
    fun loadMoreAppendsNextPage() {
        val repository = RecordingCatalogRepository(
            pages = mapOf(
                1 to gridPage(listOf(1), hasNextPage = true, currentPage = 1),
                2 to gridPage(listOf(2), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = genreHolder(repository)

        holder.load(genreCatalogFilter("Action", MediaType.ANIME))
        assertTrue(holder.media.hasNextPage)

        holder.loadMore()

        assertEquals(listOf(1, 2), holder.media.items.map { it.mediaId })
        assertFalse(holder.media.hasNextPage)
        assertEquals(2, repository.lastPage)
    }

    @Test
    fun changeTypeRequeriestheOtherCatalogFromPageOne() {
        val repository = RecordingCatalogRepository(
            pages = mapOf(1 to gridPage(listOf(1), hasNextPage = true)),
        )
        val holder = genreHolder(repository)
        holder.load(genreCatalogFilter("Action", MediaType.ANIME))

        holder.changeType(MediaType.MANGA)

        assertEquals(MediaType.MANGA, holder.filter.type)
        assertEquals(MediaType.MANGA, repository.lastFilter?.type)
        assertEquals(2, repository.calls, "the switch re-queries page 1")
        assertEquals(1, repository.lastPage)
        assertEquals(listOf(1), holder.media.items.map { it.mediaId })
    }

    @Test
    fun returningToAVisitedCatalogRestoresItsSnapshot() {
        val repository = RecordingCatalogRepository(
            pages = mapOf(1 to gridPage(listOf(1), hasNextPage = false)),
        )
        val holder = genreHolder(repository)
        holder.load(genreCatalogFilter("Action", MediaType.ANIME))

        holder.changeType(MediaType.MANGA)
        holder.changeType(MediaType.ANIME)

        assertEquals(2, repository.calls, "the anime snapshot came from the cache")
        assertEquals(MediaType.ANIME, holder.filter.type)
    }

    @Test
    fun changeSortRequeriesFromPageOneWithTheServerSort() {
        val repository = RecordingCatalogRepository(
            pages = mapOf(
                1 to gridPage(listOf(1), hasNextPage = true, currentPage = 1),
                2 to gridPage(listOf(2), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = genreHolder(repository)
        holder.load(genreCatalogFilter("Action", MediaType.ANIME))
        holder.loadMore()
        assertEquals(listOf(1, 2), holder.media.items.map { it.mediaId })

        holder.changeSort(UserListSort.AVERAGE_SCORE, UserListOrder.DESCENDING)

        assertEquals(3, repository.calls, "the change re-queries page 1 (load, page 2, change)")
        assertEquals(1, repository.lastPage)
        assertEquals("SCORE_DESC", repository.lastSort)
        assertEquals(listOf(1), holder.media.items.map { it.mediaId }, "the loaded page 2 is dropped")
        assertEquals(1, holder.media.currentPage)
        assertEquals(UserListSort.AVERAGE_SCORE, holder.sortOption)
    }

    @Test
    fun changeSortOrderDerivesTheAscendingApiKey() {
        val repository = RecordingCatalogRepository(
            pages = mapOf(1 to gridPage(listOf(1), hasNextPage = false)),
        )
        val holder = genreHolder(repository)
        holder.load(genreCatalogFilter("Action", MediaType.ANIME))

        holder.changeSort(UserListSort.TITLE, UserListOrder.ASCENDING)

        assertEquals("TITLE_ROMAJI", repository.lastSort)
        assertEquals(2, repository.calls)
    }

    @Test
    fun repickingTheSameSelectionDoesNotRequery() {
        val repository = RecordingCatalogRepository()
        val holder = genreHolder(repository)
        holder.load(genreCatalogFilter("Action", MediaType.ANIME))

        holder.changeSort(UserListSort.POPULARITY, UserListOrder.DESCENDING)
        holder.changeType(MediaType.ANIME)

        assertEquals(1, repository.calls)
    }

    @Test
    fun aPageInFlightIsDroppedWhenTheCatalogChanges() {
        val repository = GatedPageCatalogRepository(
            pageItems = mapOf(
                MediaType.ANIME to mapOf(1 to listOf(1), 2 to listOf(99)),
                MediaType.MANGA to mapOf(1 to listOf(2)),
            ),
        )
        val holder = genreHolder(repository)
        holder.load(genreCatalogFilter("Action", MediaType.ANIME))
        assertEquals(listOf(1), holder.media.items.map { it.mediaId })

        holder.loadMore() // page 2 of anime, held at the gate
        holder.changeType(MediaType.MANGA)

        repository.gate.complete(Unit) // the stale page lands after the switch

        assertEquals(listOf(2), holder.media.items.map { it.mediaId }, "only the new catalog's page")
        assertEquals(1, holder.media.currentPage)
    }

    @Test
    fun refreshKeepsTheWorksVisibleUntilTheNewPageArrives() {
        val repository = GatedCatalogRepository()
        val holder = genreHolder(repository)
        holder.load(genreCatalogFilter("Action", MediaType.ANIME))
        assertEquals(listOf(1), holder.media.items.map { it.mediaId })

        repository.pageOne = listOf(9)
        repository.hold()
        holder.refresh()

        assertEquals(listOf(1), holder.media.items.map { it.mediaId }, "the works stay on screen")

        repository.release()

        assertEquals(listOf(9), holder.media.items.map { it.mediaId }, "the response replaces them")
    }

    @Test
    fun changeTypeClearsTheWorksWhileItReloads() {
        val repository = GatedCatalogRepository()
        val holder = genreHolder(repository)
        holder.load(genreCatalogFilter("Action", MediaType.ANIME))
        assertEquals(listOf(1), holder.media.items.map { it.mediaId })

        repository.pageOne = listOf(2)
        repository.hold()
        holder.changeType(MediaType.MANGA)

        assertTrue(holder.media.items.isEmpty(), "the other catalog invalidates the works")

        repository.release()

        assertEquals(listOf(2), holder.media.items.map { it.mediaId })
    }

    @Test
    fun setHideAdultRequeriesWithTheNewGate() {
        val repository = RecordingCatalogRepository(
            pages = mapOf(1 to gridPage(listOf(1))),
        )
        val holder = genreHolder(repository, hideAdult = true)
        holder.load(genreCatalogFilter("Action", MediaType.ANIME))
        assertEquals(true, repository.lastHideAdult)

        holder.setHideAdult(false)

        assertEquals(2, repository.calls)
        assertEquals(false, repository.lastHideAdult)

        holder.setHideAdult(false) // re-picking the same value never requeries

        assertEquals(2, repository.calls)
    }

    @Test
    fun aCancelledFirstPageDoesNotStopTheCurrentSpinner() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        val repository = CancelSurvivingCatalogRepository()
        val holder = CatalogGridStateHolder(repository, scope, hideAdult = true)
        val firstGate = repository.expect(listOf(1))
        val secondGate = repository.expect(listOf(2))

        try {
            holder.load(genreCatalogFilter("Action", MediaType.ANIME)) // job 1
            dispatcher.runAll()
            assertTrue(holder.isLoading)

            // A sort change cancels job 1 and starts job 2.
            holder.changeSort(UserListSort.AVERAGE_SCORE, UserListOrder.DESCENDING)
            dispatcher.runAll()

            // The cancelled job survives cancellation (non-cooperative repo) and
            // tries to write: only job 2 may own the grid and the spinner.
            firstGate.complete(Unit)
            dispatcher.runAll()

            assertTrue(holder.isLoading, "the stale job must not clear the current spinner")
            assertTrue(holder.media.items.isEmpty(), "nor write its stale page")

            secondGate.complete(Unit)
            dispatcher.runAll()

            assertFalse(holder.isLoading)
            assertEquals(listOf(2), holder.media.items.map { it.mediaId })
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun restoringFromCacheWhilePageOneIsInFlightStopsTheSpinner() {
        val repository = CancelSurvivingCatalogRepository()
        val holder = genreHolder(repository) // Unconfined

        val bGate = repository.expect(listOf(1))
        holder.load(genreCatalogFilter("B", MediaType.ANIME))
        bGate.complete(Unit)
        assertEquals(listOf(1), holder.media.items.map { it.mediaId })

        val aGate = repository.expect(listOf(2))
        holder.load(genreCatalogFilter("A", MediaType.ANIME)) // page 1 in flight
        assertTrue(holder.isLoading)

        // B comes from the cache: the in-flight A is cancelled without a
        // relaunch, and the restore turns the spinner off itself.
        holder.load(genreCatalogFilter("B", MediaType.ANIME))
        assertFalse(holder.isLoading, "the cache restore owns the spinner")
        assertEquals(listOf(1), holder.media.items.map { it.mediaId })

        aGate.complete(Unit) // A's late page must not land
        assertFalse(holder.isLoading)
        assertEquals(listOf(1), holder.media.items.map { it.mediaId })
    }
}

/**
 * Double whose page 2 is held until [gate] completes, so a page in flight can be
 * interrupted by a catalog switch.
 */
private class GatedPageCatalogRepository(
    private val pageItems: Map<MediaType, Map<Int, List<Int>>>,
) : FakeCatalogExploreRepository() {
    val gate = CompletableDeferred<Unit>()

    override suspend fun searchMediaGrid(
        filter: MediaCatalogFilter,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> {
        if (page > 1) gate.await()
        return NetworkResult.Success(
            gridPage(
                items = pageItems[filter.type]?.get(page).orEmpty(),
                hasNextPage = page == 1,
                currentPage = page,
            ),
        )
    }
}

/**
 * Page-1 double held until [release], so the "in flight" state of a reload is
 * observable; [pageOne] lets the test change what the next response carries.
 */
private class GatedCatalogRepository : FakeCatalogExploreRepository() {
    var pageOne: List<Int> = listOf(1)
    private var gated = false
    private var gate = CompletableDeferred<Unit>()

    fun hold() {
        gate = CompletableDeferred()
        gated = true
    }

    fun release() = gate.complete(Unit)

    override suspend fun searchMediaGrid(
        filter: MediaCatalogFilter,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> {
        if (gated) gate.await()
        return NetworkResult.Success(gridPage(pageOne, hasNextPage = false, currentPage = 1))
    }
}

/**
 * Page-1 double that survives cancellation (a non-cooperative repo): a cancelled
 * request keeps going and returns, so the holder's job-identity check is what
 * stops the stale write. `expect` pre-registers one gate + payload per call.
 */
private class CancelSurvivingCatalogRepository : FakeCatalogExploreRepository() {
    private val gates = mutableListOf<CompletableDeferred<Unit>>()
    private val payloads = mutableListOf<List<Int>>()
    private var calls = 0

    fun expect(items: List<Int>): CompletableDeferred<Unit> {
        val gate = CompletableDeferred<Unit>()
        gates += gate
        payloads += items
        return gate
    }

    override suspend fun searchMediaGrid(
        filter: MediaCatalogFilter,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> {
        val index = calls++
        try {
            gates[index].await()
        } catch (_: CancellationException) {
            // Keep going: simulate a client that maps a cancellation to a value.
        }
        return NetworkResult.Success(gridPage(payloads[index], hasNextPage = false, currentPage = 1))
    }
}
