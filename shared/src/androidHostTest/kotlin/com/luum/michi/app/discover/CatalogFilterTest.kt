package com.luum.michi.app.discover

import com.luum.michi.app.core.media.domain.MediaSeason
import com.luum.michi.app.core.media.domain.MediaSeasonYear
import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.medialist.domain.UserListOrder
import com.luum.michi.app.core.medialist.domain.UserListSort
import com.luum.michi.app.core.medialist.domain.toMediaSort
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.catalogFilter
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The rail → catalog-filter mapping. Each kind must reproduce exactly the
 * criteria of its GraphQL alias in `DashboardRepositoryImpl`, including the
 * default sort.
 */
class CatalogFilterTest {

    private val now = MediaSeasonYear(MediaSeason.FALL, 2026)
    private val recent = 20251018

    private fun filter(kind: DashboardSectionKind) = kind.catalogFilter(now = now, recentStart = recent)

    @Test
    fun seasonalUsesTheCurrentSeason() {
        val f = filter(DashboardSectionKind.SEASONAL)

        assertEquals(MediaType.ANIME, f.type)
        assertEquals("FALL", f.season)
        assertEquals(2026, f.seasonYear)
        assertEquals(UserListSort.POPULARITY, f.defaultSortOption)
        assertEquals(UserListOrder.DESCENDING, f.defaultSortOrder)
    }

    @Test
    fun upcomingSeasonAdvancesTheSeason() {
        val f = filter(DashboardSectionKind.UPCOMING_SEASON)

        assertEquals("WINTER", f.season)
        assertEquals(2027, f.seasonYear)
    }

    @Test
    fun popularMovieIsAMovieCatalogByPopularity() {
        val f = filter(DashboardSectionKind.POPULAR_MOVIE)

        assertEquals(MediaType.ANIME, f.type)
        assertEquals(listOf("MOVIE"), f.formats)
        assertEquals(emptyList(), f.countries)
        assertEquals(UserListSort.POPULARITY, f.defaultSortOption)
    }

    @Test
    fun bestAnimeNowIsReleasingWithTheScoreFloor() {
        val f = filter(DashboardSectionKind.BEST_ANIME_NOW)

        assertEquals(listOf("TV", "TV_SHORT", "OVA", "ONA", "SPECIAL"), f.formats)
        assertEquals("RELEASING", f.status)
        assertEquals(5000, f.popularityGreater)
        assertEquals(UserListSort.AVERAGE_SCORE, f.defaultSortOption)
    }

    @Test
    fun bestMovieNowUsesTheRecentWindow() {
        val f = filter(DashboardSectionKind.BEST_MOVIE_NOW)

        assertEquals(listOf("MOVIE"), f.formats)
        assertEquals(recent, f.startDateGreater)
        assertEquals(5000, f.popularityGreater)
    }

    @Test
    fun newReleasesMangaUsesTheNarrowerFloor() {
        val f = filter(DashboardSectionKind.NEW_RELEASES_MANGA)

        assertEquals(MediaType.MANGA, f.type)
        assertEquals(recent, f.startDateGreater)
        assertEquals(1000, f.popularityGreater)
        assertEquals(UserListSort.AVERAGE_SCORE, f.defaultSortOption)
    }

    @Test
    fun popularManhwaIsKorean() {
        val f = filter(DashboardSectionKind.POPULAR_MANHWA)

        assertEquals(listOf("KR"), f.countries)
        assertEquals(listOf("MANGA", "ONE_SHOT"), f.formats)
    }

    @Test
    fun popularManhuaIsChinese() {
        val f = filter(DashboardSectionKind.POPULAR_MANHUA)

        assertEquals(listOf("CN", "TW"), f.countries)
        assertEquals(listOf("MANGA", "ONE_SHOT"), f.formats)
    }

    @Test
    fun bestLightNovelIsANovelWithTheScoreFloor() {
        val f = filter(DashboardSectionKind.BEST_LIGHT_NOVEL)

        assertEquals(listOf("NOVEL"), f.formats)
        assertEquals(5000, f.popularityGreater)
        assertEquals(UserListSort.AVERAGE_SCORE, f.defaultSortOption)
    }

    /** The default sort must derive to exactly the alias's `MediaSort` key. */
    @Test
    fun everyKindDefaultsToItsDashboardSortKey() {
        val expected = mapOf(
            DashboardSectionKind.SEASONAL to "POPULARITY_DESC",
            DashboardSectionKind.UPCOMING_SEASON to "POPULARITY_DESC",
            DashboardSectionKind.TRENDING_ANIME to "TRENDING_DESC",
            DashboardSectionKind.POPULAR_ANIME to "POPULARITY_DESC",
            DashboardSectionKind.POPULAR_MOVIE to "POPULARITY_DESC",
            DashboardSectionKind.BEST_ANIME to "SCORE_DESC",
            DashboardSectionKind.BEST_MOVIE to "SCORE_DESC",
            DashboardSectionKind.BEST_ANIME_NOW to "SCORE_DESC",
            DashboardSectionKind.BEST_MOVIE_NOW to "SCORE_DESC",
            DashboardSectionKind.NEW_RELEASES_MANGA to "SCORE_DESC",
            DashboardSectionKind.TRENDING_MANGA to "TRENDING_DESC",
            DashboardSectionKind.POPULAR_MANGA to "POPULARITY_DESC",
            DashboardSectionKind.POPULAR_LIGHT_NOVEL to "POPULARITY_DESC",
            DashboardSectionKind.POPULAR_MANHWA to "POPULARITY_DESC",
            DashboardSectionKind.POPULAR_MANHUA to "POPULARITY_DESC",
            DashboardSectionKind.BEST_MANGA to "SCORE_DESC",
            DashboardSectionKind.BEST_LIGHT_NOVEL to "SCORE_DESC",
            DashboardSectionKind.BEST_MANHWA to "SCORE_DESC",
            DashboardSectionKind.BEST_MANHUA to "SCORE_DESC",
        )
        DashboardSectionKind.entries.forEach { kind ->
            val f = filter(kind)
            val sortKey = f.defaultSortOption.toMediaSort(
                f.defaultSortOrder,
                isAnime = f.type == MediaType.ANIME,
            )
            assertEquals(expected.getValue(kind), sortKey, "sort key for $kind")
        }
    }
}
