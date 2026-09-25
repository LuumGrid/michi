package com.luum.michi.app.profile

import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.profile.ui.state.ProfileFavoritesGridStateHolder
import com.luum.michi.app.profile.ui.state.ProfileStateHolder
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertEquals("A", holder.favorites.anime.single().title)
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
        assertEquals("A", holder.mediaItems.single().title)
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
        assertEquals("M", holder.mediaItems.single().title)
    }

    @Test
    fun gridSliceLoadsCharactersBranch() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPage("characters", "C", false)))
        val holder = ProfileFavoritesGridStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, ProfileFavoritesCategory.CHARACTERS)
        assertEquals("C", holder.personItems.single().name)
    }

    @Test
    fun gridSliceLoadsStaffBranch() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPage("staff", "S", false)))
        val holder = ProfileFavoritesGridStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, ProfileFavoritesCategory.STAFF)
        assertEquals("S", holder.personItems.single().name)
    }

    @Test
    fun gridSliceLoadsStudiosBranch() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPage("studios", "T", false)))
        val holder = ProfileFavoritesGridStateHolder(
            ProfileRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, ProfileFavoritesCategory.STUDIOS)
        assertEquals("T", holder.studioItems.single().name)
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
        assertEquals(listOf("A", "B"), holder.mediaItems.map { it.title })
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
        assertTrue(holder.mediaItems.isEmpty())
        assertNull(holder.error)
    }
}
