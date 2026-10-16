package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaItem
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaPage
import com.luum.michi.app.mediaDetail.ui.studio.state.StudioDetailStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun studioMediaItem(
    id: Int,
    format: String? = "TV",
) = StudioMediaItem(
    mediaId = id,
    title = "Media $id",
    cover = null,
    paletteHex = null,
    format = format,
    year = 2020,
    averageScore = null,
    viewerStatus = null,
    favourites = 42,
    genres = listOf("Action", "Comedy"),
)

private fun studioDetail(
    id: Int,
    media: StudioMediaPage = StudioMediaPage(emptyList(), hasNextPage = false, currentPage = 1),
    isFavourite: Boolean = false,
) = StudioDetail(
    id = id,
    name = "Studio $id",
    isAnimeStudio = true,
    favourites = null,
    isFavourite = isFavourite,
    media = media,
)

/**
 * Repository double: canned media pages per page number plus call counters and
 * the last sort key, to prove cache hits and that a sort change re-queries.
 */
private class RecordingStudioRepository(
    private val mediaPages: Map<Int, StudioMediaPage> = emptyMap(),
) : StudioDetailRepository {
    var detailCalls = 0
        private set
    var mediaCalls = 0
        private set
    var lastSort: String? = null
        private set
    private var favourite = false

    override suspend fun loadDetail(id: Int, sort: String): NetworkResult<StudioDetail> {
        detailCalls++
        lastSort = sort
        return NetworkResult.Success(
            studioDetail(
                id = id,
                media = mediaPages[1] ?: StudioMediaPage(emptyList(), false, 1),
                isFavourite = favourite,
            ),
        )
    }

    override suspend fun loadMediaPage(
        id: Int,
        page: Int,
        sort: String,
    ): NetworkResult<StudioMediaPage> {
        mediaCalls++
        lastSort = sort
        return NetworkResult.Success(mediaPages[page] ?: StudioMediaPage(emptyList(), false, page))
    }

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> {
        favourite = !favourite
        return NetworkResult.Success(Unit)
    }
}

private fun holderWired(repository: StudioDetailRepository) = StudioDetailStateHolder(
    repository = repository,
    scope = CoroutineScope(Dispatchers.Unconfined),
    viewerId = 0,
)

class StudioDetailIntegrationTest {

    @Test
    fun loadHydratesDetailAndMediaByReleaseDate() {
        val repository = RecordingStudioRepository(
            mediaPages = mapOf(1 to StudioMediaPage(listOf(studioMediaItem(7)), false, 1)),
        )
        val holder = holderWired(repository)

        holder.load(3)

        assertEquals("Studio 3", holder.detail?.name)
        assertEquals(listOf(7), holder.mediaItems.map { it.mediaId })
        val item = holder.mediaItems.single()
        assertEquals(42, item.favourites)
        assertEquals(listOf("Action", "Comedy"), item.genres)
        assertFalse(holder.isLoading)
        assertNull(holder.error)
        assertEquals(1, repository.detailCalls)
        // Default selection: a studio opens ordered by release date, descending.
        assertEquals(UserListSort.RELEASE_DATE, holder.sortOption)
        assertEquals(UserListOrder.DESCENDING, holder.sortOrder)
        assertEquals("START_DATE_DESC", repository.lastSort)
    }

    @Test
    fun cacheHitSkipsNetworkOnRevisit() {
        val repository = RecordingStudioRepository()
        val holder = holderWired(repository)

        holder.load(1)
        holder.load(2)
        holder.load(1)

        assertEquals(2, repository.detailCalls)
        assertEquals(1, holder.detail?.id)
    }

    @Test
    fun loadMoreAppendsNextPage() {
        val repository = RecordingStudioRepository(
            mediaPages = mapOf(
                1 to StudioMediaPage(listOf(studioMediaItem(1)), hasNextPage = true, currentPage = 1),
                2 to StudioMediaPage(listOf(studioMediaItem(2)), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = holderWired(repository)

        holder.load(1)
        assertTrue(holder.hasNextPage)

        holder.loadMore()

        assertEquals(listOf(1, 2), holder.mediaItems.map { it.mediaId })
        assertFalse(holder.hasNextPage)
        assertEquals(1, repository.mediaCalls)
    }

    @Test
    fun changeSortRequeriesFromPageOneWithTheServerSort() {
        val repository = RecordingStudioRepository(
            mediaPages = mapOf(
                1 to StudioMediaPage(listOf(studioMediaItem(1)), hasNextPage = true, currentPage = 1),
                2 to StudioMediaPage(listOf(studioMediaItem(2)), hasNextPage = false, currentPage = 2),
            ),
        )
        val holder = holderWired(repository)
        holder.load(1)
        holder.loadMore()
        assertEquals(listOf(1, 2), holder.mediaItems.map { it.mediaId })

        holder.changeSort(UserListSort.AVERAGE_SCORE, UserListOrder.DESCENDING)

        assertEquals(2, repository.detailCalls, "the change re-queries page 1")
        assertEquals("SCORE_DESC", repository.lastSort)
        assertEquals(listOf(1), holder.mediaItems.map { it.mediaId }, "the loaded page 2 is dropped")
        assertEquals(1, holder.currentPage)
        assertTrue(holder.hasNextPage)
        assertEquals(UserListSort.AVERAGE_SCORE, holder.sortOption)
        assertEquals(UserListOrder.DESCENDING, holder.sortOrder)
    }

    @Test
    fun changeSortOrderDerivesTheAscendingApiKey() {
        val repository = RecordingStudioRepository(
            mediaPages = mapOf(1 to StudioMediaPage(listOf(studioMediaItem(1)), false, 1)),
        )
        val holder = holderWired(repository)
        holder.load(1)

        holder.changeSort(UserListSort.TITLE, UserListOrder.ASCENDING)

        assertEquals("TITLE_ROMAJI", repository.lastSort)
        assertEquals(2, repository.detailCalls)
    }

    @Test
    fun repickingTheSameSelectionDoesNotRequery() {
        val repository = RecordingStudioRepository()
        val holder = holderWired(repository)
        holder.load(1)

        holder.changeSort(UserListSort.RELEASE_DATE, UserListOrder.DESCENDING)

        assertEquals(1, repository.detailCalls)
    }

    @Test
    fun aPageInFlightIsDroppedWhenTheSortChanges() {
        val repository = GatedPageStudioRepository(
            pageItems = mapOf(
                "START_DATE_DESC" to mapOf(1 to listOf(1), 2 to listOf(99)),
                "SCORE_DESC" to mapOf(1 to listOf(2)),
            ),
        )
        val holder = holderWired(repository)
        holder.load(1)
        assertEquals(listOf(1), holder.mediaItems.map { it.mediaId })

        holder.loadMore() // page 2 of the default sort, held at the gate
        holder.changeSort(UserListSort.AVERAGE_SCORE, UserListOrder.DESCENDING)

        repository.gate.complete(Unit) // the stale page lands after the sort change

        assertEquals(listOf(2), holder.mediaItems.map { it.mediaId }, "only the new sort's page")
        assertEquals(1, holder.currentPage)
    }

    @Test
    fun aPageInFlightIsDroppedWhenAnotherStudioLoads() {
        val repository = GatedPageStudioRepository(
            pageItems = mapOf(
                "START_DATE_DESC" to mapOf(1 to listOf(1), 2 to listOf(99)),
            ),
        )
        val holder = holderWired(repository)
        holder.load(1)
        assertEquals(listOf(1), holder.mediaItems.map { it.mediaId })

        holder.loadMore() // page 2, held at the gate
        holder.load(2) // another studio cancels it

        repository.gate.complete(Unit) // the stale page lands after the reload

        assertEquals(listOf(1), holder.mediaItems.map { it.mediaId }, "the stale page never appends")
        assertEquals(2, holder.detail?.id)
    }

    @Test
    fun refreshKeepsTheWorksVisibleUntilTheNewPageArrives() {
        val repository = GatedDetailStudioRepository()
        val holder = holderWired(repository)
        holder.load(1)
        assertEquals(listOf(1), holder.mediaItems.map { it.mediaId })

        repository.pageOne = listOf(9)
        repository.hold()
        holder.refresh()

        assertEquals(listOf(1), holder.mediaItems.map { it.mediaId }, "the works stay on screen")

        repository.release()

        assertEquals(listOf(9), holder.mediaItems.map { it.mediaId }, "the response replaces them")
    }

    @Test
    fun changeSortClearsTheWorksWhileItReloads() {
        val repository = GatedDetailStudioRepository()
        val holder = holderWired(repository)
        holder.load(1)
        assertEquals(listOf(1), holder.mediaItems.map { it.mediaId })

        repository.pageOne = listOf(2)
        repository.hold()
        holder.changeSort(UserListSort.AVERAGE_SCORE, UserListOrder.DESCENDING)

        assertTrue(holder.mediaItems.isEmpty(), "a new order invalidates the loaded works")

        repository.release()

        assertEquals(listOf(2), holder.mediaItems.map { it.mediaId })
    }

    @Test
    fun toggleFavouriteFlips() {
        val repository = RecordingStudioRepository()
        val holder = holderWired(repository)

        holder.load(1)
        assertFalse(holder.isFavourite)

        holder.toggleFavourite()
        assertTrue(holder.isFavourite)

        holder.toggleFavourite()
        assertFalse(holder.isFavourite)
    }
}

/**
 * Sort-aware double whose media page is held until [gate] completes, so a page
 * in flight can be interrupted by a sort change.
 */
private class GatedPageStudioRepository(
    private val pageItems: Map<String, Map<Int, List<Int>>>,
) : StudioDetailRepository {
    val gate = CompletableDeferred<Unit>()

    override suspend fun loadDetail(id: Int, sort: String): NetworkResult<StudioDetail> {
        val items = pageItems[sort]?.get(1).orEmpty().map { studioMediaItem(it) }
        return NetworkResult.Success(
            studioDetail(id = id, media = StudioMediaPage(items, hasNextPage = true, currentPage = 1)),
        )
    }

    override suspend fun loadMediaPage(
        id: Int,
        page: Int,
        sort: String,
    ): NetworkResult<StudioMediaPage> {
        gate.await()
        val items = pageItems[sort]?.get(page).orEmpty().map { studioMediaItem(it) }
        return NetworkResult.Success(StudioMediaPage(items, hasNextPage = false, currentPage = page))
    }

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> = NetworkResult.Success(Unit)
}

private class GatedStudioRepository : StudioDetailRepository by RecordingStudioRepository() {
    val gate = CompletableDeferred<Unit>()

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> {
        gate.await()
        return NetworkResult.Success(Unit)
    }
}

/**
 * Detail-page double held until [release], so the "in flight" state of a reload
 * is observable; [pageOne] lets the test change what the next response carries.
 */
private class GatedDetailStudioRepository : StudioDetailRepository {
    var pageOne: List<Int> = listOf(1)
    private var gated = false
    private var gate = CompletableDeferred<Unit>()

    fun hold() {
        gate = CompletableDeferred()
        gated = true
    }

    fun release() = gate.complete(Unit)

    override suspend fun loadDetail(id: Int, sort: String): NetworkResult<StudioDetail> {
        if (gated) gate.await()
        return NetworkResult.Success(
            studioDetail(
                id = id,
                media = StudioMediaPage(
                    items = pageOne.map { studioMediaItem(it) },
                    hasNextPage = false,
                    currentPage = 1,
                ),
            ),
        )
    }

    override suspend fun loadMediaPage(
        id: Int,
        page: Int,
        sort: String,
    ): NetworkResult<StudioMediaPage> =
        NetworkResult.Success(StudioMediaPage(emptyList(), hasNextPage = false, currentPage = page))

    override suspend fun toggleFavourite(id: Int): NetworkResult<Unit> = NetworkResult.Success(Unit)
}

/** See `MediaDetailFavouriteCacheTest`: the late response must not mix entries. */
class StudioDetailFavouriteCacheTest {

    @Test
    fun togglingOnAnotherStudioKeepsEachCacheEntrySeparate() {
        val repository = GatedStudioRepository()
        val holder = holderWired(repository)

        holder.load(1)
        assertFalse(holder.isFavourite)
        holder.toggleFavourite()
        assertTrue(holder.isFavourite, "optimistic")

        holder.load(2)
        assertFalse(holder.isFavourite, "the visible studio is the second one")

        repository.gate.complete(Unit)

        assertFalse(holder.isFavourite, "the late response does not touch the visible one")
        holder.load(1)
        assertTrue(holder.isFavourite, "the first studio's snapshot kept the toggle")
    }
}
