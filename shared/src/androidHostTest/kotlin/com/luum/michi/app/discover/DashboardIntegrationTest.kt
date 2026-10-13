package com.luum.michi.app.discover

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.discover.repository.DashboardRepositoryImpl
import com.luum.michi.app.discover.ui.dashboard.state.DashboardStateHolder
import com.luum.michi.app.discover.ui.dashboard.state.createDashboardStateHolder
import com.luum.michi.app.mediaList.FakeGraphQL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun mediaJson(
    id: Int,
    title: String,
    format: String = "TV",
    episodes: Int? = 12,
    chapters: Int? = null,
): JsonObject = buildJsonObject {
    put("id", id)
    put("title", buildJsonObject { put("romaji", title) })
    put("format", format)
    if (episodes != null) put("episodes", episodes)
    if (chapters != null) put("chapters", chapters)
    put("coverImage", buildJsonObject { put("large", "https://img/$id.jpg") })
}

private fun page(vararg media: JsonObject): JsonObject =
    buildJsonObject { put("media", JsonArray(media.toList())) }

private fun dashboardJson(
    trendingAnime: List<JsonObject> = emptyList(),
    trendingManga: List<JsonObject> = emptyList(),
    newReleasesManga: List<JsonObject> = emptyList(),
    thisSeason: List<JsonObject> = emptyList(),
    upcomingNextSeason: List<JsonObject> = emptyList(),
    allTimePopularAnime: List<JsonObject> = emptyList(),
    allTimePopularManga: List<JsonObject> = emptyList(),
    topAnime: List<JsonObject> = emptyList(),
    topManga: List<JsonObject> = emptyList(),
): JsonElement = buildJsonObject {
    put("trendingAnime", page(*trendingAnime.toTypedArray()))
    put("trendingManga", page(*trendingManga.toTypedArray()))
    put("newReleasesManga", page(*newReleasesManga.toTypedArray()))
    put("thisSeason", page(*thisSeason.toTypedArray()))
    put("upcomingNextSeason", page(*upcomingNextSeason.toTypedArray()))
    put("allTimePopularAnime", page(*allTimePopularAnime.toTypedArray()))
    put("allTimePopularManga", page(*allTimePopularManga.toTypedArray()))
    put("topAnime", page(*topAnime.toTypedArray()))
    put("topManga", page(*topManga.toTypedArray()))
}

private fun wired(graphQL: FakeGraphQL): DashboardStateHolder =
    createDashboardStateHolder(
        repository = DashboardRepositoryImpl(graphQL),
        scope = CoroutineScope(Dispatchers.Unconfined),
        strings = EnglishStrings,
    )

class DashboardIntegrationTest {

    @Test
    fun mapsEverySectionIntoItsHolderList() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(
                    trendingAnime = listOf(mediaJson(1, "Trending A")),
                    trendingManga = listOf(mediaJson(2, "Trending M", format = "MANGA", episodes = null, chapters = 40)),
                    newReleasesManga = listOf(mediaJson(9, "New M", format = "MANGA", episodes = null, chapters = 30)),
                    thisSeason = listOf(mediaJson(3, "Season A")),
                    upcomingNextSeason = listOf(mediaJson(4, "Next A")),
                    allTimePopularAnime = listOf(mediaJson(5, "Popular A")),
                    allTimePopularManga = listOf(mediaJson(6, "Popular M", format = "MANGA", episodes = null, chapters = 40)),
                    topAnime = listOf(mediaJson(7, "Top A")),
                    topManga = listOf(mediaJson(8, "Top M", format = "MANGA", episodes = null, chapters = 40)),
                ),
            ),
        )
        val holder = wired(graphQL)

        holder.load()

        assertEquals(listOf(1), holder.trendingAnime.map { it.id })
        assertEquals(listOf(2), holder.trendingManga.map { it.id })
        assertEquals(listOf(9), holder.newReleasesManga.map { it.id })
        assertEquals(listOf(3), holder.thisSeason.map { it.id })
        assertEquals(listOf(4), holder.upcomingNextSeason.map { it.id })
        assertEquals(listOf(5), holder.allTimePopularAnime.map { it.id })
        assertEquals(listOf(6), holder.allTimePopularManga.map { it.id })
        assertEquals(listOf(7), holder.topAnime.map { it.id })
        assertEquals(listOf(8), holder.topManga.map { it.id })
        assertEquals("Trending A", holder.trendingAnime.single().title)
        // meta subtitle: format · episode/chapter count
        assertEquals("TV · 12 ep", holder.trendingAnime.single().meta)
        assertTrue(holder.trendingManga.single().meta.endsWith("40 ch"))
    }

    @Test
    fun hideAdultPinsIsAdultFalse() {
        val graphQL = FakeGraphQL(
            listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))),
        )
        val holder = wired(graphQL)

        // Default is hidden: a no-op sync, then the first load carries the gate.
        holder.setHideAdult(true)
        holder.load()

        assertEquals(1, graphQL.calls)
        val variables = graphQL.requests.single().variables
        assertEquals("false", variables?.get("isAdult")?.toString())
    }

    @Test
    fun adultAllowedOmitsIsAdultVariable() {
        val graphQL = FakeGraphQL(
            listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))),
        )
        val holder = wired(graphQL)

        // A change forces the reload (no separate load() needed here).
        holder.setHideAdult(false)

        assertEquals(1, graphQL.calls)
        val variables = graphQL.requests.single().variables
        assertFalse(variables?.containsKey("isAdult") == true, "isAdult must be omitted (null = both)")
    }

    @Test
    fun secondLoadWithinTtlSkipsTheNetwork() {
        val graphQL = FakeGraphQL(
            listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))),
        )
        val holder = wired(graphQL)

        holder.load()
        holder.load()

        assertEquals(1, graphQL.calls)
        assertEquals(1, holder.trendingAnime.size)
    }

    @Test
    fun forceRefreshReloadsWithinTtl() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingAnime = listOf(mediaJson(2, "B"))),
            ),
        )
        val holder = wired(graphQL)

        holder.load()
        holder.load(forceRefresh = true)

        assertEquals(2, graphQL.calls)
        assertEquals(listOf(2), holder.trendingAnime.map { it.id })
    }

    @Test
    fun newReleasesCarriesTheRecentWindowVariable() {
        val graphQL = FakeGraphQL(
            listOf(dashboardJson(newReleasesManga = listOf(mediaJson(9, "New M", format = "MANGA", episodes = null, chapters = 30)))),
        )
        val holder = wired(graphQL)

        holder.load()

        val request = graphQL.requests.single()
        assertTrue(request.query.contains("newReleasesManga"))
        assertTrue(request.query.contains("startDate_greater"))
        val start = request.variables?.get("recentMangaStart")?.toString()?.toIntOrNull()
        assertTrue(start != null && start in 19_000_000..30_000_000, "FuzzyDateInt window present: $start")
    }

    @Test
    fun factoryDoesNotLoadOnCreation() {
        val graphQL = FakeGraphQL(
            listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))),
        )

        wired(graphQL)

        assertEquals(0, graphQL.calls, "creating the holder must not fire a request")
    }
}
