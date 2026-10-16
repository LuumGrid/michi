package com.luum.michi.app.profile

import com.luum.michi.app.DeferredDispatcher
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.profile.ui.state.ProfileFavoritesGridStateHolder
import com.luum.michi.app.profile.ui.state.ProfileStateHolder
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileIntegrationTest {

    @Test
    fun happySlicePopulatesHolder() {
        val graphQL = FakeProfileGraphQL(listOf(profileData()))
        val holder = ProfileStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1)
        assertEquals(10, holder.stats.animeCount)
        assertEquals(9, holder.stats.followingCount)
        assertEquals("A", holder.favorites.anime.single().name)
        assertNull(holder.error)
    }

    @Test
    fun nullUserEndToEndStaysUsable() {
        val graphQL = FakeProfileGraphQL(listOf(buildJsonObject {}))
        val holder = ProfileStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1)
        assertEquals(0, holder.stats.animeCount)
        assertTrue(holder.favorites.anime.isEmpty())
        assertNull(holder.error)
    }

    @Test
    fun gridSliceLoadsThroughRealRepo() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPage("anime", "A", true)))
        val holder = ProfileFavoritesGridStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, ProfileFavoritesCategory.ANIME)
        assertEquals("A", holder.items.single().name)
        assertTrue(holder.hasNextPage)
        assertNull(holder.error)
    }

    @Test
    fun serverFailureSurfacesOnHolder() {
        val graphQL = FakeProfileGraphQL(listOf(profileData()), failureAt = 0)
        val holder = ProfileStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1)
        assertEquals(0, holder.stats.animeCount)
        assertTrue(holder.error != null)
    }

    @Test
    fun refreshRefetchesThroughRealRepo() {
        val graphQL = FakeProfileGraphQL(
            listOf(profileData(), profileData()),
        )
        val holder = ProfileStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1)
        holder.refresh()
        assertEquals(2, graphQL.calls)
        assertEquals(10, holder.stats.animeCount)
    }

    @Test
    fun gridSliceLoadsMangaBranch() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPage("manga", "M", false)))
        val holder = ProfileFavoritesGridStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, ProfileFavoritesCategory.MANGA)
        assertEquals("M", holder.items.single().name)
    }

    @Test
    fun gridSliceLoadsCharactersBranch() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPage("characters", "C", false)))
        val holder = ProfileFavoritesGridStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, ProfileFavoritesCategory.CHARACTERS)
        assertEquals("C", holder.items.single().name)
    }

    @Test
    fun gridSliceLoadsStaffBranch() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPage("staff", "S", false)))
        val holder = ProfileFavoritesGridStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, ProfileFavoritesCategory.STAFF)
        assertEquals("S", holder.items.single().name)
    }

    @Test
    fun gridSliceLoadsStudiosBranch() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPage("studios", "T", false)))
        val holder = ProfileFavoritesGridStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, ProfileFavoritesCategory.STUDIOS)
        assertEquals("T", holder.items.single().name)
    }

    @Test
    fun gridPaginationAppendsSecondPage() {
        val graphQL = FakeProfileGraphQL(
            listOf(
                favouritesPage("anime", "A", hasNextPage = true, id = 2),
                favouritesPage("anime", "B", hasNextPage = false, id = 3),
            ),
        )
        val holder = ProfileFavoritesGridStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, ProfileFavoritesCategory.ANIME)
        holder.loadMore()
        assertEquals(listOf("A", "B"), holder.items.map { it.name })
        assertEquals(false, holder.hasNextPage)
    }

    @Test
    fun emptyGridStaysEmptyWithoutError() {
        val graphQL = FakeProfileGraphQL(
            listOf(
                buildJsonObject {
                    put("User", buildJsonObject { put("favourites", buildJsonObject {}) })
                },
            ),
        )
        val holder = ProfileFavoritesGridStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, ProfileFavoritesCategory.MANGA)
        assertTrue(holder.items.isEmpty())
        assertNull(holder.error)
    }

    // ────────────── races at the repository seam ──────────────

    @Test
    fun lastUserWinsClearsThePreviousDataAndOwnsTheSpinner() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeProfileGraphQL(
                listOf(profileData(animeCount = 10), profileData(animeCount = 20), profileData(animeCount = 30)),
            )
            val holder = ProfileStateHolder(ProfileRepositoryImpl(client), scope)

            holder.load(1) // completes: user 1 on screen
            dispatcher.runAll()
            assertEquals(10, holder.stats.animeCount)

            client.gate = CompletableDeferred()
            holder.load(2) // gated
            dispatcher.runAll()
            assertTrue(holder.isLoading)
            assertEquals(0, holder.stats.animeCount, "user 1 data must not show while user 2 loads")

            holder.load(3) // cancels user 2, still gated
            dispatcher.runAll()
            assertTrue(holder.isLoading, "the stale finally must not stop the current spinner")

            client.gate!!.complete(Unit)
            dispatcher.runAll()

            assertFalse(holder.isLoading)
            assertEquals(30, holder.stats.animeCount, "the last user wins")
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun refreshRetriesTheRequestedUserAfterAFailedSwitch() {
        val graphQL = FakeProfileGraphQL(
            listOf(profileData(animeCount = 10), profileData(animeCount = 0), profileData(animeCount = 20)),
            failureAt = 1,
        )
        val holder = ProfileStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )

        holder.load(1)
        assertEquals(10, holder.stats.animeCount)

        holder.load(2) // fails: the screen is now empty, lastUserId is still 1
        assertTrue(holder.error != null)

        holder.refresh()

        val userId = (graphQL.requests.last().variables?.get("userId") as? JsonPrimitive)?.content
        assertEquals("2", userId, "refresh retries the requested user, not the last success")
        assertEquals(20, holder.stats.animeCount)
    }

    @Test
    fun gridCategoryChangeDropsAnInFlightPageAndItsSpinner() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeProfileGraphQL(
                listOf(
                    favouritesPage("anime", "anime-1", hasNextPage = true),
                    favouritesPageOf("anime", hasNextPage = false, 99 to "anime-2"),
                    favouritesPage("manga", "manga-1", hasNextPage = false),
                ),
            )
            val holder = ProfileFavoritesGridStateHolder(ProfileRepositoryImpl(client), scope)

            holder.load(1, ProfileFavoritesCategory.ANIME)
            dispatcher.runAll()
            assertEquals(listOf("anime-1"), holder.items.map { it.name })

            client.gate = CompletableDeferred()
            holder.loadMore() // page 2 of anime, held at the gate
            dispatcher.runAll()
            assertTrue(holder.isLoadingMore)

            holder.load(1, ProfileFavoritesCategory.MANGA)
            dispatcher.runAll()
            assertFalse(holder.isLoadingMore, "the category change drops the page in flight")

            client.gate!!.complete(Unit)
            dispatcher.runAll()

            assertEquals(listOf("manga-1"), holder.items.map { it.name }, "the stale page never lands")
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun gridDeduplicatesOverlappingPages() {
        val graphQL = FakeProfileGraphQL(
            listOf(
                favouritesPageOf("anime", hasNextPage = true, 1 to "A", 2 to "B"),
                favouritesPageOf("anime", hasNextPage = false, 2 to "B", 3 to "C"),
            ),
        )
        val holder = ProfileFavoritesGridStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, ProfileFavoritesCategory.ANIME)
        holder.loadMore()
        assertEquals(listOf("A", "B", "C"), holder.items.map { it.name })
    }
}
