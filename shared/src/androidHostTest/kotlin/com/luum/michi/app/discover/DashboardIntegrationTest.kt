package com.luum.michi.app.discover

import com.luum.michi.app.core.language.domain.EnglishStrings
import com.luum.michi.app.core.model.RailPageSize
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.repository.DashboardRepositoryImpl
import com.luum.michi.app.discover.repository.clearLegacyDashboardCache
import com.luum.michi.app.discover.ui.dashboard.state.DashboardStateHolder
import com.luum.michi.app.discover.ui.dashboard.state.createDashboardStateHolder
import com.luum.michi.app.settings.FakeSettingsStore
import com.luum.michi.app.FakeGraphQL
import com.luum.michi.app.DeferredDispatcher
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

private fun wired(
    graphQL: FakeGraphQL,
    hideAdult: Boolean = true,
    initialType: DashboardMediaType = DashboardMediaType.ANIME,
): DashboardStateHolder =
    createDashboardStateHolder(
        repository = DashboardRepositoryImpl(graphQL),
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
    fun selectingTheOtherTypeLoadsItAndKeepsIt() {
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

        // Back to anime: served from memory, no request.
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
        holder.selectType(DashboardMediaType.ANIME) // memory
        assertEquals(2, graphQL.calls)

        holder.setHideAdult(false) // both rails dropped, current reloaded
        assertEquals(3, graphQL.calls)

        holder.selectType(DashboardMediaType.MANGA) // no longer in memory
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
    fun invalidateThenReloadFetchesAgain() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingAnime = listOf(mediaJson(2, "B"))),
            ),
        )
        val holder = wired(graphQL)

        holder.load() // call 1
        holder.invalidate() // language change: drop memory, launch nothing
        holder.load() // the screen composes again

        assertEquals(2, graphQL.calls)
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
    }

    @Test
    fun invalidateCancelsTheInFlightLoadSoTheNextLoadIsNotDropped() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingAnime = listOf(mediaJson(2, "B"))),
            ),
        ).apply { gate = CompletableDeferred() }
        val holder = wired(graphQL)
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
                dashboardJson(trendingManga = listOf(mediaJson(2, "M", format = "MANGA", episodes = null, chapters = 1))),
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
            ),
        )
        val holder = wired(graphQL)

        // Manga loads first (call 0), held at the gate so its in-flight job is
        // registered before it completes and cleanly clears its own indicator.
        val mangaGate = CompletableDeferred<Unit>()
        graphQL.gate = mangaGate
        holder.selectType(DashboardMediaType.MANGA)
        mangaGate.complete(Unit)
        assertFalse(holder.isLoading, "the manga rail is in memory")

        graphQL.gate = CompletableDeferred()
        val animeGate = graphQL.gate!!
        holder.selectType(DashboardMediaType.ANIME) // anime in flight (call 1)
        assertTrue(holder.isLoading, "the selected type is loading")

        holder.selectType(DashboardMediaType.MANGA) // memory, within TTL: no request, no spinner
        assertFalse(holder.isLoading, "the manga rail came from memory")
        assertFalse(holder.isRefreshing)

        graphQL.gate = null
        animeGate.complete(Unit)

        assertEquals(2, graphQL.calls)
        assertFalse(holder.isLoading, "the anime request finished without touching manga's indicator")
        assertFalse(holder.isRefreshing)
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_MANGA]?.map { it.id })
    }

    @Test
    fun aSynchronousResponseClearsTheSpinner() {
        // Unconfined runs the request as soon as it is started: the job is stored
        // before its body runs, so a synchronous response still clears its own
        // indicator instead of leaving the spinner on.
        val graphQL = FakeGraphQL(listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))))
        val holder = wired(graphQL)

        holder.load()

        assertFalse(holder.isLoading, "a synchronously-resolved load must clear its own spinner")
        assertFalse(holder.isRefreshing)
        assertEquals(listOf(1), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
    }

    @Test
    fun invalidateDoesNotLoadUntilComposed() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingAnime = listOf(mediaJson(2, "B"))),
            ),
        )
        val holder = wired(graphQL)

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
    fun initialTypeFromTheSettingDoesNotLoad() {
        val graphQL = FakeGraphQL(emptyList()) // any call would crash
        val holder = wired(graphQL, initialType = DashboardMediaType.MANGA)

        assertEquals(DashboardMediaType.MANGA, holder.mediaType)
        assertEquals(0, graphQL.calls, "creating the holder never fires a request")
    }

    @Test
    fun factoryDoesNotLoadOnCreation() {
        val graphQL = FakeGraphQL(listOf(dashboardJson(trendingAnime = listOf(mediaJson(1, "A")))))

        wired(graphQL)

        assertEquals(0, graphQL.calls, "creating the holder must not fire a request")
    }

    @Test
    fun aColdStartHolderAlwaysHitsTheNetwork() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingAnime = listOf(mediaJson(2, "B"))),
            ),
        )
        val first = wired(graphQL)
        first.load()
        assertEquals(1, graphQL.calls)

        // A new holder is a cold start: it has no memory, so it always fetches
        // even though another holder already loaded the same rails.
        val second = wired(graphQL)
        second.load()

        assertEquals(2, graphQL.calls, "a fresh holder never reuses another holder's memory")
        assertEquals(listOf(2), second.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
    }

    @Test
    fun clearingLegacyDashboardCacheBlanksOnlyLoadedKeysOnce() {
        val store = FakeSettingsStore().apply {
            putString("dashboard_ANIME_true", """{"savedAtMillis":1,"sections":[]}""")
            putString("dashboard_ANIME_false", """{"savedAtMillis":1,"sections":[]}""")
            putString("dashboard_MANGA_true", """{"savedAtMillis":1,"sections":[]}""")
            putString("dashboard_MANGA_false", """{"savedAtMillis":1,"sections":[]}""")
            putString("theme_mode", "DARK")
        }

        clearLegacyDashboardCache(store)

        assertEquals("", store.getString("dashboard_ANIME_true"))
        assertEquals("", store.getString("dashboard_ANIME_false"))
        assertEquals("", store.getString("dashboard_MANGA_true"))
        assertEquals("", store.getString("dashboard_MANGA_false"))
        assertEquals("DARK", store.getString("theme_mode"), "other keys are untouched")
        assertNull(store.getString("default_home_tab"), "an absent key stays absent")

        val writesAfterFirstRun = store.putStringCount
        clearLegacyDashboardCache(store)

        assertEquals(
            writesAfterFirstRun,
            store.putStringCount,
            "a second run finds every dashboard key already blank and writes nothing",
        )
    }

    @Test
    fun aNeverLoadedTypeCountsAsLoadingAndInvalidateRestoresThat() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(trendingAnime = listOf(mediaJson(2, "B"))),
            ),
        )
        // DeferredDispatcher: a real dispatcher on which the request does not run
        // inline, so the mid-load frame is observable.
        val dispatcher = DeferredDispatcher()
        val holder = createDashboardStateHolder(
            repository = DashboardRepositoryImpl(graphQL),
            scope = CoroutineScope(dispatcher),
            hideAdult = true,
        ) { EnglishStrings }

        assertTrue(
            holder.isLoading,
            "a fresh holder has no feed and no error: the first frame is loading, not empty",
        )

        holder.load()
        dispatcher.runAll()
        assertFalse(holder.isLoading)

        holder.invalidate() // language change drops the feed
        assertTrue(holder.isLoading, "the frame before the recompose-triggered load is loading again")

        holder.load() // the screen composes again
        dispatcher.runAll()
        assertFalse(holder.isLoading, "the spinner goes off once the reload lands")
        assertEquals(listOf(2), holder.sections[DashboardSectionKind.TRENDING_ANIME]?.map { it.id })
    }

    @Test
    fun aFailedPreloadSelectedByTheUserSurfacesTheError() {
        val graphQL = FakeGraphQL(
            listOf(
                dashboardJson(trendingAnime = listOf(mediaJson(1, "A"))),
                dashboardJson(newReleasesManga = listOf(mediaJson(2, "M", format = "MANGA"))),
            ),
            failureAt = 1,
        )
        val holder = wired(graphQL)
        holder.load() // anime, call 0

        graphQL.gate = CompletableDeferred()
        holder.preloadOther() // manga preload in flight (call 1, will fail)
        val gate = graphQL.gate!!
        holder.selectType(DashboardMediaType.MANGA) // joins the doomed preload

        graphQL.gate = null
        gate.complete(Unit)

        assertEquals(2, graphQL.calls, "the select joined the preload instead of firing again")
        assertTrue(
            holder.error != null,
            "a failed preload must surface once its type is the visible one",
        )
        assertFalse(
            holder.isLoading,
            "there is an error to show, so the type is not stuck on the spinner",
        )
    }
}
