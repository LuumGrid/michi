package com.luum.michi.app.mediaDetail

import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioDetail
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaItem
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaPage
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaSort
import com.luum.michi.app.mediaDetail.ui.studio.state.StudioDetailStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun studioMediaItem(id: Int) = StudioMediaItem(
    mediaId = id,
    title = "Media $id",
    coverUrl = null,
    paletteHex = null,
    format = "TV",
    year = 2020,
    averageScore = null,
    viewerStatus = null,
    type = "ANIME",
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
 * Repository double: canned media pages per page number plus call counters to
 * prove cache hits and sort reloads.
 */
private class RecordingStudioRepository(
    private val mediaPages: Map<Int, StudioMediaPage> = emptyMap(),
) : StudioDetailRepository {
    var detailCalls = 0
        private set
    var mediaCalls = 0
        private set
    var lastSort: StudioMediaSort? = null
        private set
    private var favourite = false

    override suspend fun loadDetail(id: Int, sort: StudioMediaSort): NetworkResult<StudioDetail> {
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
        sort: StudioMediaSort,
    ): NetworkResult<StudioMediaPage> {
        mediaCalls++
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
    fun loadHydratesDetailAndMedia() {
        val repository = RecordingStudioRepository(
            mediaPages = mapOf(1 to StudioMediaPage(listOf(studioMediaItem(7)), false, 1)),
        )
        val holder = holderWired(repository)

        holder.load(3)

        assertEquals("Studio 3", holder.detail?.name)
        assertEquals(listOf(7), holder.mediaItems.map { it.mediaId })
        val item = holder.mediaItems.single()
        assertEquals("ANIME", item.type)
        assertEquals(42, item.favourites)
        assertEquals(listOf("Action", "Comedy"), item.genres)
        assertFalse(holder.isLoading)
        assertNull(holder.error)
        assertEquals(1, repository.detailCalls)
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
    fun changeSortAppliesLocallyWithoutReload() {
        val repository = RecordingStudioRepository()
        val holder = holderWired(repository)

        holder.load(1)
        holder.changeSort(StudioMediaSort.SCORE)

        assertEquals(StudioMediaSort.SCORE, holder.sort)
        // Display sort is client-side: the server is always queried by popularity.
        assertEquals(StudioMediaSort.POPULARITY, repository.lastSort)
        assertEquals(1, repository.detailCalls)
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
