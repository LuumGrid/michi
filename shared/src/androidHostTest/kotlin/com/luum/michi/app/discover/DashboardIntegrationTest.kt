package com.luum.michi.app.discover

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.network.domain.RailPageSize
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.repository.DashboardRepositoryImpl
import com.luum.michi.app.discover.ui.dashboard.state.DashboardStateHolder
import com.luum.michi.app.discover.ui.dashboard.state.createDashboardStateHolder
import com.luum.michi.app.discover.repository.SettingsStoreDashboardFeedCache
import com.luum.michi.app.settings.FakeSettingsStore
import com.luum.michi.app.mediaList.FakeGraphQL
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.luum.michi.app.discover.domain.CachedDashboardFeed
import com.luum.michi.app.discover.domain.DashboardFeed
import com.luum.michi.app.discover.domain.DashboardFeedCache
import com.luum.michi.app.discover.domain.model.MediaItem
import kotlin.time.Clock

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
    seasonal: List<JsonObject> = emptyList(),
    upcomingSeason: List<JsonObject> = emptyList(),
    trendingAnime: List<JsonObject> = emptyList(),
    popularAnime: List<JsonObject> = emptyList(),
    popularMovie: List<JsonObject> = emptyList(),
    bestAnime: List<JsonObject> = emptyList(),
    bestMovie: List<JsonObject> = emptyList(),
    bestAnimeNow: List<JsonObject> = emptyList(),
    bestMovieNow: List<JsonObject> = emptyList(),
    newReleasesManga: List<JsonObject> = emptyList(),
    trendingManga: List<JsonObject> = emptyList(),
    popularManga: List<JsonObject> = emptyList(),
    popularLightNovel: List<JsonObject> = emptyList(),
    popularManhwa: List<JsonObject> = emptyList(),
    popularManhua: List<JsonObject> = emptyList(),
    bestManga: List<JsonObject> = emptyList(),
    bestLightNovel: List<JsonObject> = emptyList(),
    bestManhwa: List<JsonObject> = emptyList(),
    bestManhua: List<JsonObject> = emptyList(),
): JsonElement = buildJsonObject {
    put("seasonal", page(*seasonal.toTypedArray()))
    put("upcomingSeason", page(*upcomingSeason.toTypedArray()))
    put("trendingAnime", page(*trendingAnime.toTypedArray()))
    put("popularAnime", page(*popularAnime.toTypedArray()))
    put("popularMovie", page(*popularMovie.toTypedArray()))
    put("bestAnime", page(*bestAnime.toTypedArray()))
    put("bestMovie", page(*bestMovie.toTypedArray()))
    put("bestAnimeNow", page(*bestAnimeNow.toTypedArray()))
    put("bestMovieNow", page(*bestMovieNow.toTypedArray()))
    put("newReleasesManga", page(*newReleasesManga.toTypedArray()))
    put("trendingManga", page(*trendingManga.toTypedArray()))
    put("popularManga", page(*popularManga.toTypedArray()))
    put("popularLightNovel", page(*popularLightNovel.toTypedArray()))
    put("popularManhwa", page(*popularManhwa.toTypedArray()))
    put("popularManhua", page(*popularManhua.toTypedArray()))
    put("bestManga", page(*bestManga.toTypedArray()))
    put("bestLightNovel", page(*bestLightNovel.toTypedArray()))
    put("bestManhwa", page(*bestManhwa.toTypedArray()))
    put("bestManhua", page(*bestManhua.toTypedArray()))
}

private class InMemoryDashboardFeedCache : DashboardFeedCache {
    private val store = mutableMapOf<String, CachedDashboardFeed>()

    override fun load(mediaType: DashboardMediaType, hideAdult: Boolean): CachedDashboardFeed? =
        store["${mediaType.name}_$hideAdult"]

    override fun save(mediaType: DashboardMediaType, hideAdult: Boolean, feed: DashboardFeed) {
        store["${mediaType.name}_$hideAdult"] = CachedDashboardFeed(
            feed = feed,
            savedAtMillis = Clock.System.now().toEpochMilliseconds(),
        )
    }

    override fun clear() {
        store.clear()
    }
}

private fun wired(graphQL: FakeGraphQL, hideAdult: Boolean = true): DashboardStateHolder =
    wiredWithCache(graphQL, InMemoryDashboardFeedCache(), hideAdult)

private fun wiredWithCache(
    graphQL: FakeGraphQL,
    cache: DashboardFeedCache,
    hideAdult: Boolean = true,
    initialType: DashboardMediaType = DashboardMediaType.ANIME,
): DashboardStateHolder =
    createDashboardStateHolder(
        repository = DashboardRepositoryImpl(graphQL),
        cache = cache,
        scope = CoroutineScope(Dispatchers.Unconfined),
        hideAdult = hideAdult,
        initialType = initialType,
    ) { EnglishStrings }

class DashboardIntegrationTest {

    @Test
    fun mapsAnimeSectionsIntoTheirKinds() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(
                    seasonal = listOf(mediaJson(1, "Season A")),
                    upcomingSeason = listOf(mediaJson(2, "Next A")),
                    trendingAnime = listOf(mediaJson(3, "Rising A")),
                    popularMovie = listOf(mediaJson(4, "Pop Movie", format = "MOVIE")),
                    bestMovieNow = listOf(mediaJson(5, "Now Movie", format = "MOVIE")),
                ),
            ),
        )
        val holder = wired(graphQL)

        holder.load()

        assertEquals(listOf(1), holder.sections[DashboardSectionKind.SEASONAL]?.map { it.id })
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.UPCOMING_SEASON]?.map { it.id })
        assertEquals(listOf(3), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
        assertEquals(listOf(4), holder.sections[DashboardSectionKind.POPULAR_MOVIE]?.map { it.id })
        assertEquals(listOf(5), holder.sections[DashboardSectionKind.BEST_MOVIE_NOW]?.map { it.id })
        // absent aliases never appear
        assertTrue(holder.sections[DashboardSectionKind.POPULAR_ANIME] == null)
        assertEquals("TV · 12 ep", holder.sections[DashboardSectionKind.SEASONAL]!!.single().meta)
    }

    @Test
    fun mapsMangaSectionsIntoTheirKinds() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(
                    newReleasesManga = listOf(mediaJson(1, "New M", format = "MANGA", episodes = null, chapters = 30)),
                    popularManhua = listOf(mediaJson(2, "Manhua", format = "MANGA", episodes = null, chapters = 100)),
                    bestLightNovel = listOf(mediaJson(3, "LN", format = "NOVEL", episodes = null, chapters = 5)),
                ),
            ),
        )
        val holder = wired(graphQL)
        holder.selectType(DashboardMediaType.MANGA)

        assertEquals(listOf(1), holder.sections[DashboardSectionKind.NEW_RELEASES_MANGA]?.map { it.id })
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.POPULAR_MANHUA]?.map { it.id })
        assertEquals(listOf(3), holder.sections[DashboardSectionKind.BEST_LIGHT_NOVEL]?.map { it.id })
    }

    @Test
    fun selectingTheOtherTypeLoadsItAndCaches() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingManga = listOf(mediaJson(2, "M", format = "MANGA", episodes = null, chapters = 9))),
            ),
        )
        val holder = wired(graphQL)

        holder.load()
        assertEquals(1, graphQL.calls)

        holder.selectType(DashboardMediaType.MANGA)
        assertEquals(2, graphQL.calls)
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_MANGA]?.map { it.id })

        // Back to anime: served from the per-type cache, no request.
        holder.selectType(DashboardMediaType.ANIME)
        assertEquals(2, graphQL.calls)
        assertEquals(listOf(1), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
    }

    @Test
    fun hideAdultPinsIsAdultFalse() {
        val graphQL = FakeGraphQL(listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))))
        val holder = wired(graphQL)

        holder.setHideAdult(true)
        holder.load()

        assertEquals(1, graphQL.calls)
        assertEquals("false", graphQL.requests.single().variables?.get("isAdult")?.toString())
    }

    @Test
    fun adultAllowedOmitsIsAdultVariable() {
        val graphQL = FakeGraphQL(listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))))
        val holder = wired(graphQL)

        holder.setHideAdult(false)

        assertEquals(1, graphQL.calls)
        assertFalse(graphQL.requests.single().variables?.containsKey("isAdult") == true, "isAdult must be omitted (null = both)")
    }

    @Test
    fun secondLoadWithinTtlSkipsTheNetwork() {
        val graphQL = FakeGraphQL(listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))))
        val holder = wired(graphQL)

        holder.load()
        holder.load()

        assertEquals(1, graphQL.calls)
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
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
    }

    @Test
    fun newReleasesCarriesTheRecentWindowAndPopularityFloor() {
        val graphQL = FakeGraphQL(
            listOf(dashboardJson(newReleasesManga = listOf(mediaJson(9, "New M", format = "MANGA", episodes = null, chapters = 30)))),
        )
        val holder = wired(graphQL)
        holder.selectType(DashboardMediaType.MANGA)

        val request = graphQL.requests.single()
        assertTrue(request.query.contains("newReleasesManga"))
        assertTrue(request.query.contains("startDate_greater"))
        assertTrue(request.query.contains("popularity_greater"), "new releases needs a popularity floor")
        val start = request.variables?.get("recentMangaStart")?.toString()?.toIntOrNull()
        assertTrue(start != null && start in 19_000_000..30_000_000, "FuzzyDateInt window present: $start")
    }

    @Test
    fun queryPagesEveryAnimeRailAtRailPageSize() {
        val graphQL = FakeGraphQL(listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))))
        val holder = wired(graphQL)

        holder.load()

        assertEquals(9, Regex("perPage: $RailPageSize").findAll(graphQL.requests.single().query).count())
    }

    @Test
    fun hideAdultChangeDuringLoadAppliesToTheFinalResponse() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingAnime = listOf(mediaJson(2, "B"))),
            ),
        ).apply { gate = CompletableDeferred() }
        val holder = wired(graphQL)
        val gate = graphQL.gate!!

        holder.load() // in flight, blocked on the gate
        holder.setHideAdult(false) // requested mid-flight: must not be lost
        graphQL.gate = null
        gate.complete(Unit)

        assertEquals(2, graphQL.calls, "the deferred reload ran after the stale one")
        assertFalse(graphQL.requests.last().variables?.containsKey("isAdult") == true, "final load uses the new hideAdult")
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
    }

    @Test
    fun selectTypeDuringLoadLoadsTheNewType() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingManga = listOf(mediaJson(2, "M", format = "MANGA", episodes = null, chapters = 9))),
            ),
        ).apply { gate = CompletableDeferred() }
        val holder = wired(graphQL)
        val gate = graphQL.gate!!

        holder.load() // anime in flight
        holder.selectType(DashboardMediaType.MANGA) // mid-flight: must not be lost
        graphQL.gate = null
        gate.complete(Unit)

        assertEquals(2, graphQL.calls, "the deferred load for the new type ran")
        assertEquals(DashboardMediaType.MANGA, holder.mediaType)
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_MANGA]?.map { it.id })
    }

    @Test
    fun setHideAdultDropsBothTypes() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingManga = listOf(mediaJson(2, "M", format = "MANGA", episodes = null, chapters = 9))),
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingManga = listOf(mediaJson(2, "M", format = "MANGA", episodes = null, chapters = 9))),
            ),
        )
        val holder = wired(graphQL)

        holder.load()
        holder.selectType(DashboardMediaType.MANGA)
        holder.selectType(DashboardMediaType.ANIME) // cached
        assertEquals(2, graphQL.calls)

        holder.setHideAdult(false) // both caches dropped, current reloaded
        assertEquals(3, graphQL.calls)

        holder.selectType(DashboardMediaType.MANGA) // no longer cached
        assertEquals(4, graphQL.calls)
    }

    @Test
    fun errorIsPerType() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingManga = listOf(mediaJson(2, "M", format = "MANGA", episodes = null, chapters = 9))),
            ),
            failureAt = 0,
        )
        val holder = wired(graphQL)

        holder.load()
        assertTrue(holder.error != null, "anime failed")

        holder.selectType(DashboardMediaType.MANGA)
        assertEquals(null, holder.error, "an anime failure never shows on manga")
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_MANGA]?.map { it.id })
    }

    @Test
    fun animeMainRailsExcludeMoviesAndMangaRailsStayJapanese() {
        val animeGraphQL = FakeGraphQL(listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))))
        wired(animeGraphQL).load()
        val animeQuery = animeGraphQL.requests.single().query
        assertTrue(animeQuery.contains("format_in: [TV, TV_SHORT, OVA, ONA, SPECIAL]"))
        assertFalse(animeQuery.contains("countryOfOrigin"))

        val mangaGraphQL = FakeGraphQL(
            listOf(dashboardJson(trendingManga = listOf(mediaJson(2, "M", format = "MANGA", episodes = null, chapters = 1)))),
        )
        wired(mangaGraphQL).selectType(DashboardMediaType.MANGA)
        val mangaQuery = mangaGraphQL.requests.single().query
        assertTrue(mangaQuery.contains("countryOfOrigin: JP"))
        assertTrue(mangaQuery.contains("format_in: [MANGA, ONE_SHOT]"))
    }

    @Test
    fun hydratesFromTheDiskCacheWithoutAFetch() {
        val cache = InMemoryDashboardFeedCache().apply {
            save(
                mediaType = DashboardMediaType.MANGA,
                hideAdult = true,
                feed = DashboardFeed(
                    mapOf(
                        DashboardSectionKind.TRENDING_MANGA to listOf(
                            MediaItem(title = "Cached", meta = "Manga", id = 7),
                        ),
                    ),
                ),
            )
        }
        val graphQL = FakeGraphQL(emptyList()) // any call would crash
        val holder = wiredWithCache(graphQL, cache)

        holder.selectType(DashboardMediaType.MANGA)

        assertEquals(listOf(7), holder.sections[DashboardSectionKind.TRENDING_MANGA]?.map { it.id })
        assertEquals(0, graphQL.calls, "a fresh cache skips the network")
    }

    @Test
    fun preloadOtherWarmsTheOtherTypeOnce() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingManga = listOf(mediaJson(2, "M", format = "MANGA", episodes = null, chapters = 1))),
            ),
        )
        val holder = wired(graphQL)

        holder.load()
        assertEquals(1, graphQL.calls)

        holder.preloadOther()
        assertEquals(2, graphQL.calls)

        // The warmed type is served from memory (no extra fetch).
        holder.selectType(DashboardMediaType.MANGA)
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_MANGA]?.map { it.id })
        assertEquals(2, graphQL.calls)

        holder.preloadOther() // other type already warm: guarded
        assertEquals(2, graphQL.calls)
    }

    @Test
    fun coldStartHydratesFromCacheWithoutASpinner() {
        val cache = InMemoryDashboardFeedCache().apply {
            save(
                mediaType = DashboardMediaType.ANIME,
                hideAdult = true,
                feed = DashboardFeed(
                    mapOf(DashboardSectionKind.TRENDING_ANIME to listOf(MediaItem(title = "Cached", meta = "Anime", id = 5))),
                ),
            )
        }
        val graphQL = FakeGraphQL(emptyList())
        val holder = wiredWithCache(graphQL, cache)

        holder.load()

        assertEquals(listOf(5), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
        assertFalse(holder.isLoading, "a fresh cache shows content, not a spinner")
        assertEquals(0, graphQL.calls)
    }

    @Test
    fun gateChangeDuringLoadDiscardsTheStaleResponse() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "Old"))),
                dashboardJson(trendingAnime = listOf(mediaJson(2, "New"))),
            ),
        ).apply { gate = CompletableDeferred() }
        val holder = wired(graphQL)
        val gate = graphQL.gate!!

        holder.load() // in flight with gate = true
        holder.setHideAdult(false) // gate flips mid-flight
        graphQL.gate = null
        gate.complete(Unit)

        assertEquals(2, graphQL.calls, "the deferred reload ran with the new gate")
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
    }

    @Test
    fun preloadWithFreshCacheSkipsTheNetwork() {
        val cache = InMemoryDashboardFeedCache().apply {
            save(
                mediaType = DashboardMediaType.MANGA,
                hideAdult = true,
                feed = DashboardFeed(
                    mapOf(DashboardSectionKind.TRENDING_MANGA to listOf(MediaItem(title = "Cached", meta = "Manga", id = 7))),
                ),
            )
        }
        val graphQL = FakeGraphQL(listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))))
        val holder = wiredWithCache(graphQL, cache)

        holder.load() // anime only
        holder.preloadOther() // manga is fresh in cache -> no request

        assertEquals(1, graphQL.calls)
    }

    @Test
    fun selectDuringPreloadJoinsInsteadOfDuplicating() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingManga = listOf(mediaJson(2, "M", format = "MANGA", episodes = null, chapters = 1))),
            ),
        )
        val holder = wired(graphQL)
        holder.load() // anime (call 1)

        graphQL.gate = CompletableDeferred()
        holder.preloadOther() // manga in flight (call 2)
        val gate = graphQL.gate!!
        holder.selectType(DashboardMediaType.MANGA) // joins the in-flight preload

        graphQL.gate = null
        gate.complete(Unit)

        assertEquals(2, graphQL.calls, "the select joined the preload instead of fetching again")
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_MANGA]?.map { it.id })
    }

    @Test
    fun invalidateThenReloadRewritesTheCache() {
        val cache = InMemoryDashboardFeedCache()
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingAnime = listOf(mediaJson(2, "B"))),
            ),
        )
        val holder = wiredWithCache(graphQL, cache)

        holder.load() // call 1, writes the cache
        holder.invalidate() // language change: drop disk + memory, launch nothing
        holder.load() // the screen composes again

        assertEquals(2, graphQL.calls)
        // The reload rewrote the cache with the fresh (re-localized) feed.
        assertEquals(
            listOf(2),
            cache.load(DashboardMediaType.ANIME, true)
                ?.feed?.sections?.get(DashboardSectionKind.TRENDING_ANIME)?.map { it.id },
        )
    }

    @Test
    fun invalidateCancelsTheInFlightLoadSoTheNextLoadIsNotDropped() {
        val cache = InMemoryDashboardFeedCache()
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingAnime = listOf(mediaJson(2, "B"))),
            ),
        ).apply { gate = CompletableDeferred() }
        val holder = wiredWithCache(graphQL, cache)
        val gate = graphQL.gate!!

        holder.load() // in flight, held at the gate
        holder.invalidate() // language change: cancels the doomed request

        graphQL.gate = null
        gate.complete(Unit)
        holder.load() // the screen composes again: fires, does not join

        assertEquals(2, graphQL.calls, "the stale request was cancelled, a fresh one ran")
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
    }

    @Test
    fun loadingIsTrackedPerType() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingManga = listOf(mediaJson(2, "M", format = "MANGA", episodes = null, chapters = 1))),
            ),
        ).apply { gate = CompletableDeferred() }
        val cache = InMemoryDashboardFeedCache().apply {
            save(
                mediaType = DashboardMediaType.MANGA,
                hideAdult = true,
                feed = DashboardFeed(
                    mapOf(DashboardSectionKind.TRENDING_MANGA to listOf(MediaItem(title = "Cached", meta = "Manga", id = 7))),
                ),
            )
        }
        val holder = wiredWithCache(graphQL, cache)
        val gate = graphQL.gate!!

        holder.load() // anime in flight
        assertTrue(holder.isLoading, "the selected type is loading")

        holder.selectType(DashboardMediaType.MANGA) // fresh cache: no request, no spinner
        assertFalse(holder.isLoading, "the manga rail came from the cache")
        assertFalse(holder.isRefreshing)

        graphQL.gate = null
        gate.complete(Unit)

        assertFalse(holder.isLoading, "the anime request finished without touching manga's indicator")
        assertFalse(holder.isRefreshing)
        assertEquals(listOf(7), holder.sections[DashboardSectionKind.TRENDING_MANGA]?.map { it.id })
    }

    @Test
    fun gateChangeWithFreshNewKeyCacheSkipsTheNetwork() {
        val cache = InMemoryDashboardFeedCache().apply {
            save(
                mediaType = DashboardMediaType.ANIME,
                hideAdult = false,
                feed = DashboardFeed(
                    mapOf(DashboardSectionKind.TRENDING_ANIME to listOf(MediaItem(title = "Cached", meta = "Anime", id = 9))),
                ),
            )
        }
        val graphQL = FakeGraphQL(emptyList()) // any call would crash
        val holder = wiredWithCache(graphQL, cache, hideAdult = true)

        holder.setHideAdult(false) // the new gate's cache is fresh: hydrate, no network

        assertEquals(0, graphQL.calls, "a fresh cache under the new gate skips the network")
        assertEquals(listOf(9), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
    }

    @Test
    fun invalidateDoesNotLoadUntilComposed() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingAnime = listOf(mediaJson(2, "B"))),
            ),
        )
        val cache = InMemoryDashboardFeedCache()
        val holder = wiredWithCache(graphQL, cache)

        holder.load()
        assertEquals(1, graphQL.calls)
        val before = holder.generation

        holder.invalidate() // language change while another tab is visible

        assertEquals(1, graphQL.calls, "invalidate() launches nothing")
        assertTrue(holder.generation != before, "the generation is observable so the screen can re-key")
        assertTrue(holder.sections.isEmpty(), "the stale-language rails are dropped")

        holder.load() // the screen is composed again

        assertEquals(2, graphQL.calls)
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
    }

    @Test
    fun staleGenerationDoesNotWriteDisk() {
        val cache = InMemoryDashboardFeedCache()
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingManga = listOf(mediaJson(1, "Stale"))),
                dashboardJson(trendingManga = listOf(mediaJson(2, "Fresh"))),
            ),
        ).apply { gate = CompletableDeferred() }
        val holder = wiredWithCache(graphQL, cache)
        val gate = graphQL.gate!!

        holder.selectType(DashboardMediaType.MANGA) // call 0, blocked
        holder.invalidate() // language change mid-flight
        graphQL.gate = null
        gate.complete(Unit)

        assertNull(cache.load(DashboardMediaType.MANGA, true), "the previous generation never wrote the disk cache")
        assertTrue(holder.sections.isEmpty(), "nor reached the in-memory rails")

        holder.load() // the screen is composed again

        assertEquals(2, graphQL.calls)
        assertEquals(DashboardMediaType.MANGA, holder.mediaType, "the selection survived the language change")
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_MANGA]?.map { it.id })
        assertEquals(
            listOf(2),
            cache.load(DashboardMediaType.MANGA, true)
                ?.feed?.sections?.get(DashboardSectionKind.TRENDING_MANGA)?.map { it.id },
        )
    }

    @Test
    fun initialTypeFromTheSettingDoesNotLoad() {
        val graphQL = FakeGraphQL(emptyList()) // any call would crash
        val holder = wiredWithCache(
            graphQL = graphQL,
            cache = InMemoryDashboardFeedCache(),
            initialType = DashboardMediaType.MANGA,
        )

        assertEquals(DashboardMediaType.MANGA, holder.mediaType)
        assertEquals(0, graphQL.calls, "creating the holder never fires a request")
    }

    @Test
    fun aCacheWrittenBeforeStatsExistedStillDecodes() {
        val store = FakeSettingsStore().apply {
            putString(
                "dashboard_ANIME_true",
                """{"savedAtMillis":1000,"sections":[{"kind":"TRENDING_ANIME",""" +
                    """"items":[{"title":"Old","meta":"TV · 12 ep","id":3}]}]}""",
            )
        }
        val cache = SettingsStoreDashboardFeedCache(store)

        val item = cache.load(DashboardMediaType.ANIME, hideAdult = true)
            ?.feed?.sections?.get(DashboardSectionKind.TRENDING_ANIME)?.single()

        assertEquals(3, item?.id)
        assertNull(item?.averageScore, "an older cache has no stats")
        assertNull(item?.popularity)
        assertNull(item?.favourites)
    }

    @Test
    fun statsRoundTripThroughTheDiskCache() {
        val cache = SettingsStoreDashboardFeedCache(FakeSettingsStore())
        cache.save(
            mediaType = DashboardMediaType.ANIME,
            hideAdult = true,
            feed = DashboardFeed(
                mapOf(
                    DashboardSectionKind.BEST_ANIME to listOf(
                        MediaItem(
                            title = "A",
                            meta = "TV",
                            id = 1,
                            averageScore = 85,
                            popularity = 12_345,
                            favourites = 900,
                        ),
                    ),
                ),
            ),
        )

        val item = cache.load(DashboardMediaType.ANIME, hideAdult = true)
            ?.feed?.sections?.get(DashboardSectionKind.BEST_ANIME)?.single()

        assertEquals(85, item?.averageScore)
        assertEquals(12_345, item?.popularity)
        assertEquals(900, item?.favourites)
    }

    @Test
    fun factoryDoesNotLoadOnCreation() {
        val graphQL = FakeGraphQL(listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))))

        wired(graphQL)

        assertEquals(0, graphQL.calls, "creating the holder must not fire a request")
    }
}
