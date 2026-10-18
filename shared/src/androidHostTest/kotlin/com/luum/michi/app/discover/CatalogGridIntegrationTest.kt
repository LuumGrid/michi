package com.luum.michi.app.discover

import com.luum.michi.app.core.media.domain.MediaSeason
import com.luum.michi.app.core.media.domain.MediaSeasonYear
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.catalogFilter
import com.luum.michi.app.discover.domain.model.genreCatalogFilter
import com.luum.michi.app.discover.ui.catalog.state.CatalogGridStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Dashboard "View all" catalog and the genre page share one grid holder;
 * these cover the catalog's own behavior and that the LRU cache keeps each
 * filter separate across the detail chain.
 */
class CatalogGridIntegrationTest {

    private val now = MediaSeasonYear(MediaSeason.FALL, 2026)
    private val recent = 20251018

    private fun holder(repository: RecordingCatalogRepository) = CatalogGridStateHolder(
        repository = repository,
        scope = CoroutineScope(Dispatchers.Unconfined),
        hideAdult = true,
    )

    @Test
    fun openingACatalogUsesTheRailsDefaultSort() {
        val repository = RecordingCatalogRepository(pages = mapOf(1 to gridPage(listOf(1))))
        val state = holder(repository)
        val filter = DashboardSectionKind.BEST_ANIME.catalogFilter(now = now, recentStart = recent)

        state.load(filter)

        assertEquals(1, repository.calls)
        assertEquals(filter, repository.lastFilter)
        assertEquals(UserListSort.AVERAGE_SCORE, state.sortOption, "opens on the rail's order")
        assertEquals("SCORE_DESC", repository.lastSort)
    }

    @Test
    fun cacheHitSkipsNetworkWhenTheSameCatalogReopens() {
        val repository = RecordingCatalogRepository()
        val state = holder(repository)
        val popularAnime = DashboardSectionKind.POPULAR_ANIME.catalogFilter(now = now, recentStart = recent)

        state.load(popularAnime)
        state.load(DashboardSectionKind.POPULAR_MOVIE.catalogFilter(now = now, recentStart = recent))
        state.load(popularAnime)

        assertEquals(2, repository.calls)
        assertEquals(popularAnime, state.filter)
    }

    @Test
    fun theSameSortIsANoOpAndAnotherSortRequeriesthePageOne() {
        val repository = RecordingCatalogRepository(pages = mapOf(1 to gridPage(listOf(1))))
        val state = holder(repository)
        state.load(DashboardSectionKind.POPULAR_ANIME.catalogFilter(now = now, recentStart = recent))

        state.changeSort(UserListSort.POPULARITY, UserListOrder.DESCENDING) // already active
        assertEquals(1, repository.calls)

        state.changeSort(UserListSort.AVERAGE_SCORE, UserListOrder.DESCENDING)
        assertEquals(2, repository.calls)
        assertEquals(1, repository.lastPage)
        assertEquals("SCORE_DESC", repository.lastSort)
        // The rest of the filter is kept (the score floor stays on a best-rated catalog).
        assertEquals(
            DashboardSectionKind.POPULAR_ANIME.catalogFilter(now = now, recentStart = recent).countries,
            repository.lastFilter?.countries,
        )
    }

    @Test
    fun theCatalogComesBackFromCacheAfterAMediaAndAGenre() {
        val repository = RecordingCatalogRepository(pages = mapOf(1 to gridPage(listOf(1))))
        val state = holder(repository)
        val catalog = DashboardSectionKind.BEST_ANIME.catalogFilter(now = now, recentStart = recent)
        val genre = genreCatalogFilter("Action", MediaType.ANIME)

        // catalog → (a media opens, served by the media holder) → tap a genre →
        // back → back: the catalog restores from the cache, with its own order.
        state.load(catalog)
        state.load(genre)
        state.load(catalog)

        assertEquals(2, repository.calls, "the catalog returns from the cache")
        assertEquals(catalog, state.filter)
        assertEquals(UserListSort.AVERAGE_SCORE, state.sortOption, "with its own order")
    }

    @Test
    fun aGenreDoesNotClobberTheCachedCatalog() {
        val repository = RecordingCatalogRepository(pages = mapOf(1 to gridPage(listOf(1))))
        val state = holder(repository)
        val genre = genreCatalogFilter("Action", MediaType.MANGA)
        val catalog = DashboardSectionKind.POPULAR_MANGA.catalogFilter(now = now, recentStart = recent)

        // genre (opened from a media) → the catalog page → back to the genre:
        // both snapshots live side by side in the same LRU cache.
        state.load(genre)
        state.load(catalog)
        state.load(genre)

        assertEquals(2, repository.calls)
        assertEquals(genre, state.filter)
        assertEquals(UserListSort.POPULARITY, state.sortOption)
    }
}
