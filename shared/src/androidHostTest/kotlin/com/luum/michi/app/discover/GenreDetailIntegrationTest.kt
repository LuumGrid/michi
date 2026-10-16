package com.luum.michi.app.discover

import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.model.ExploreResult
import com.luum.michi.app.discover.ui.genre.state.GenreDetailStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun gridItem(id: Int) = MediaGridItem(
    mediaId = id,
    title = "Media $id",
    cover = null,
    paletteHex = null,
    format = "TV",
    year = 2020,
    averageScore = null,
    viewerStatus = null,
    favourites = 42,
    genres = listOf("Action", "Comedy"),
)

private fun gridPage(items: List<Int>, hasNextPage: Boolean = false, currentPage: Int = 1) =
    Page(items.map { gridItem(it) }, hasNextPage = hasNextPage, currentPage = currentPage)

private fun holderWired(
    repository: ExploreRepository,
    hideAdult: Boolean = true,
) = GenreDetailStateHolder(
    repository = repository,
    scope = CoroutineScope(Dispatchers.Unconfined),
    hideAdult = hideAdult,
)

/**
 * The genre page only uses [ExploreRepository.searchMediaGrid]; the rest of the
 * catalog interface is stubbed so the double stays a full implementation.
 */
private open class FakeGenreExploreRepository : ExploreRepository {
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
    ): NetworkResult<Page<ExploreResult>> = NetworkResult.Success(Page<ExploreResult>(emptyList(), false, 1))

    override suspend fun searchMediaGrid(
        type: MediaType,
        genres: List<String>,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> = NetworkResult.Success(Page(emptyList(), false, page))

    override suspend fun searchCharacters(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> = NetworkResult.Success(Page<ExploreResult>(emptyList(), false, 1))

    override suspend fun searchStaff(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> = NetworkResult.Success(Page<ExploreResult>(emptyList(), false, 1))

    override suspend fun searchStudios(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
    ): NetworkResult<Page<ExploreResult>> = NetworkResult.Success(Page<ExploreResult>(emptyList(), false, 1))
}

/**
 * Repository double: canned pages per page number plus call counters and the
 * last request, to prove cache hits, the adult gate and that a catalog/sort
 * change re-queries.
 */
private class RecordingGenreRepository(
    private val pages: Map<Int, Page<MediaGridItem>> = emptyMap(),
) : FakeGenreExploreRepository() {
    var calls = 0
        private set
    var lastSort: String? = null
        private set
    var lastType: MediaType? = null
        private set
    var lastPage: Int? = null
        private set
    var lastHideAdult: Boolean? = null
        private set
    var lastGenres: List<String>? = null
        private set

    override suspend fun searchMediaGrid(
        type: MediaType,
        genres: List<String>,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> {
        calls++
        lastSort = sort
        lastType = type
        lastPage = page
        lastHideAdult = hideAdult
        lastGenres = genres
        return NetworkResult.Success(pages[page] ?: Page(emptyList(), false, page))
    }
}

class GenreDetailIntegrationTest {

    @Test
    fun loadHydratesPageOneByPopularity() {
        val repository = RecordingGenreRepository(
            pages = mapOf(1 to gridPage(listOf(7), hasNextPage = true)),
        )
        val holder = holderWired(repository)

        holder.load("Action", MediaType.ANIME)

        assertEquals("Action", holder.name)
        assertEquals(MediaType.ANIME, holder.type)
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
        assertEquals(MediaType.ANIME, repository.lastType)
        assertEquals(listOf("Action"), repository.lastGenres, "the genre travels as the catalog filter")
        assertEquals(true, repository.lastHideAdult)
    }

    @Test
    fun cacheHitSkipsNetworkWhenTheSameGenreReopens() {
        val repository = RecordingGenreRepository()
        val holder = holderWired(repository)

        holder.load("Action", MediaType.ANIME)
        holder.load("Comedy", MediaType.ANIME)
        holder.load("Action", MediaType.ANIME)

        assertEquals(2, repository.calls)
        assertEquals("Action", holder.name)
    }

    @Test
    fun loadMoreAppendsNextPage() {
        val repository = RecordingGenreRepository(
            pages = mapOf(
                1 to gridPage(listOf(1), hasNextPage = true, currentPage = 1),
                2 to gridPage(listOf(2), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = holderWired(repository)

        holder.load("Action", MediaType.ANIME)
        assertTrue(holder.media.hasNextPage)

        holder.loadMore()

        assertEquals(listOf(1, 2), holder.media.items.map { it.mediaId })
        assertFalse(holder.media.hasNextPage)
        assertEquals(2, repository.lastPage)
    }

    @Test
    fun changeTypeRequeriestheOtherCatalogFromPageOne() {
        val repository = RecordingGenreRepository(
            pages = mapOf(1 to gridPage(listOf(1), hasNextPage = true)),
        )
        val holder = holderWired(repository)
        holder.load("Action", MediaType.ANIME)

        holder.changeType(MediaType.MANGA)

        assertEquals(MediaType.MANGA, holder.type)
        assertEquals(MediaType.MANGA, repository.lastType)
        assertEquals(2, repository.calls, "the switch re-queries page 1")
        assertEquals(1, repository.lastPage)
        assertEquals(listOf(1), holder.media.items.map { it.mediaId })
    }

    @Test
    fun returningToAVisitedCatalogRestoresItsSnapshot() {
        val repository = RecordingGenreRepository(
            pages = mapOf(1 to gridPage(listOf(1), hasNextPage = false)),
        )
        val holder = holderWired(repository)
        holder.load("Action", MediaType.ANIME)

        holder.changeType(MediaType.MANGA)
        holder.changeType(MediaType.ANIME)

        assertEquals(2, repository.calls, "the anime snapshot came from the cache")
        assertEquals(MediaType.ANIME, holder.type)
    }

    @Test
    fun changeSortRequeriesFromPageOneWithTheServerSort() {
        val repository = RecordingGenreRepository(
            pages = mapOf(
                1 to gridPage(listOf(1), hasNextPage = true, currentPage = 1),
                2 to gridPage(listOf(2), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = holderWired(repository)
        holder.load("Action", MediaType.ANIME)
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
        val repository = RecordingGenreRepository(
            pages = mapOf(1 to gridPage(listOf(1), hasNextPage = false)),
        )
        val holder = holderWired(repository)
        holder.load("Action", MediaType.ANIME)

        holder.changeSort(UserListSort.TITLE, UserListOrder.ASCENDING)

        assertEquals("TITLE_ROMAJI", repository.lastSort)
        assertEquals(2, repository.calls)
    }

    @Test
    fun repickingTheSameSelectionDoesNotRequery() {
        val repository = RecordingGenreRepository()
        val holder = holderWired(repository)
        holder.load("Action", MediaType.ANIME)

        holder.changeSort(UserListSort.POPULARITY, UserListOrder.DESCENDING)
        holder.changeType(MediaType.ANIME)

        assertEquals(1, repository.calls)
    }

    @Test
    fun aPageInFlightIsDroppedWhenTheCatalogChanges() {
        val repository = GatedPageGenreRepository(
            pageItems = mapOf(
                MediaType.ANIME to mapOf(1 to listOf(1), 2 to listOf(99)),
                MediaType.MANGA to mapOf(1 to listOf(2)),
            ),
        )
        val holder = holderWired(repository)
        holder.load("Action", MediaType.ANIME)
        assertEquals(listOf(1), holder.media.items.map { it.mediaId })

        holder.loadMore() // page 2 of anime, held at the gate
        holder.changeType(MediaType.MANGA)

        repository.gate.complete(Unit) // the stale page lands after the switch

        assertEquals(listOf(2), holder.media.items.map { it.mediaId }, "only the new catalog's page")
        assertEquals(1, holder.media.currentPage)
    }

    @Test
    fun refreshKeepsTheWorksVisibleUntilTheNewPageArrives() {
        val repository = GatedGenreRepository()
        val holder = holderWired(repository)
        holder.load("Action", MediaType.ANIME)
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
        val repository = GatedGenreRepository()
        val holder = holderWired(repository)
        holder.load("Action", MediaType.ANIME)
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
        val repository = RecordingGenreRepository(
            pages = mapOf(1 to gridPage(listOf(1))),
        )
        val holder = holderWired(repository, hideAdult = true)
        holder.load("Action", MediaType.ANIME)
        assertEquals(true, repository.lastHideAdult)

        holder.setHideAdult(false)

        assertEquals(2, repository.calls)
        assertEquals(false, repository.lastHideAdult)

        holder.setHideAdult(false) // re-picking the same value never requeries

        assertEquals(2, repository.calls)
    }
}

/**
 * Double whose page 2 is held until [gate] completes, so a page in flight can be
 * interrupted by a catalog switch.
 */
private class GatedPageGenreRepository(
    private val pageItems: Map<MediaType, Map<Int, List<Int>>>,
) : FakeGenreExploreRepository() {
    val gate = CompletableDeferred<Unit>()

    override suspend fun searchMediaGrid(
        type: MediaType,
        genres: List<String>,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> {
        if (page > 1) gate.await()
        return NetworkResult.Success(
            gridPage(
                items = pageItems[type]?.get(page).orEmpty(),
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
private class GatedGenreRepository : FakeGenreExploreRepository() {
    var pageOne: List<Int> = listOf(1)
    private var gated = false
    private var gate = CompletableDeferred<Unit>()

    fun hold() {
        gate = CompletableDeferred()
        gated = true
    }

    fun release() = gate.complete(Unit)

    override suspend fun searchMediaGrid(
        type: MediaType,
        genres: List<String>,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> {
        if (gated) gate.await()
        return NetworkResult.Success(gridPage(pageOne, hasNextPage = false, currentPage = 1))
    }
}
