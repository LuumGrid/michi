package com.luum.michi.app.account

import com.luum.michi.app.account.domain.model.AccountFavoritesCategory
import com.luum.michi.app.account.repository.AccountRepositoryImpl
import com.luum.michi.app.account.ui.state.AccountFavoritesGridStateHolder
import com.luum.michi.app.account.ui.state.AccountStateHolder
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountIntegrationTest {

    @Test
    fun happySlicePopulatesHolder() {
        val graphQL = FakeAccountGraphQL(listOf(accountData()))
        val holder = AccountStateHolder(
            AccountRepositoryImpl(graphQL),
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
        val graphQL = FakeAccountGraphQL(listOf(buildJsonObject {}))
        val holder = AccountStateHolder(
            AccountRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1)
        assertEquals(0, holder.stats.animeCount)
        assertTrue(holder.favorites.anime.isEmpty())
        assertNull(holder.error)
    }

    @Test
    fun gridSliceLoadsThroughRealRepo() {
        val graphQL = FakeAccountGraphQL(listOf(favouritesPage("anime", "A", true)))
        val holder = AccountFavoritesGridStateHolder(
            AccountRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, AccountFavoritesCategory.ANIME)
        assertEquals("A", holder.mediaItems.single().title)
        assertTrue(holder.hasNextPage)
        assertNull(holder.error)
    }

    @Test
    fun serverFailureSurfacesOnHolder() {
        val graphQL = FakeAccountGraphQL(listOf(accountData()), failureAt = 0)
        val holder = AccountStateHolder(
            AccountRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1)
        assertEquals(0, holder.stats.animeCount)
        assertTrue(holder.error != null)
    }

    @Test
    fun refreshRefetchesThroughRealRepo() {
        val graphQL = FakeAccountGraphQL(
            listOf(accountData(), accountData()),
        )
        val holder = AccountStateHolder(
            AccountRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1)
        holder.refresh()
        assertEquals(2, graphQL.calls)
        assertEquals(10, holder.stats.animeCount)
    }
    @Test
    fun gridSliceLoadsMangaBranch() {
        val graphQL = FakeAccountGraphQL(listOf(favouritesPage("manga", "M", false)))
        val holder = AccountFavoritesGridStateHolder(
            AccountRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, AccountFavoritesCategory.MANGA)
        assertEquals("M", holder.mediaItems.single().title)
    }

    @Test
    fun gridSliceLoadsCharactersBranch() {
        val graphQL = FakeAccountGraphQL(listOf(favouritesPage("characters", "C", false)))
        val holder = AccountFavoritesGridStateHolder(
            AccountRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, AccountFavoritesCategory.CHARACTERS)
        assertEquals("C", holder.personItems.single().name)
    }

    @Test
    fun gridSliceLoadsStaffBranch() {
        val graphQL = FakeAccountGraphQL(listOf(favouritesPage("staff", "S", false)))
        val holder = AccountFavoritesGridStateHolder(
            AccountRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, AccountFavoritesCategory.STAFF)
        assertEquals("S", holder.personItems.single().name)
    }

    @Test
    fun gridSliceLoadsStudiosBranch() {
        val graphQL = FakeAccountGraphQL(listOf(favouritesPage("studios", "T", false)))
        val holder = AccountFavoritesGridStateHolder(
            AccountRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, AccountFavoritesCategory.STUDIOS)
        assertEquals("T", holder.studioItems.single().name)
    }

    @Test
    fun gridPaginationAppendsSecondPage() {
        val graphQL = FakeAccountGraphQL(
            listOf(
                favouritesPage("anime", "A", hasNextPage = true, id = 2),
                favouritesPage("anime", "B", hasNextPage = false, id = 3),
            ),
        )
        val holder = AccountFavoritesGridStateHolder(
            AccountRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, AccountFavoritesCategory.ANIME)
        holder.loadMore()
        assertEquals(listOf("A", "B"), holder.mediaItems.map { it.title })
        assertEquals(false, holder.hasNextPage)
    }

    @Test
    fun emptyGridStaysEmptyWithoutError() {
        val graphQL = FakeAccountGraphQL(
            listOf(
                buildJsonObject {
                    put("User", buildJsonObject { put("favourites", buildJsonObject {}) })
                },
            ),
        )
        val holder = AccountFavoritesGridStateHolder(
            AccountRepositoryImpl(graphQL),
            CoroutineScope(Dispatchers.Unconfined),
        )
        holder.load(1, AccountFavoritesCategory.MANGA)
        assertTrue(holder.mediaItems.isEmpty())
        assertNull(holder.error)
    }
}
