package com.luum.michi.app.profile

import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaList.InMemoryFileStore
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.repository.ProfileDiskCache
import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.profile.ui.state.ProfileFavoritesGridStateHolder
import com.luum.michi.app.profile.ui.state.ProfileStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun gridHolder(
    graphQL: FakeProfileGraphQL,
    favourite: FakeFavouriteRepository = FakeFavouriteRepository(),
    onFavouriteChanged: (FavouriteTarget, Boolean) -> Unit = { _, _ -> },
    onFavoriteRemoved: (ProfileFavoritesCategory, Int) -> Unit = { _, _ -> },
): ProfileFavoritesGridStateHolder = ProfileFavoritesGridStateHolder(
    repository = ProfileRepositoryImpl(graphQL),
    favouriteRepository = favourite,
    scope = CoroutineScope(Dispatchers.Unconfined),
    onFavouriteChanged = onFavouriteChanged,
    onFavoriteRemoved = onFavoriteRemoved,
)

private fun favoriteNodes(vararg ids: Int): Array<Pair<Int, String>> =
    ids.map { it to "Favorite $it" }.toTypedArray()

private fun rangeNodes(range: IntRange): Array<Pair<Int, String>> =
    favoriteNodes(*range.toList().toIntArray())

private fun requestedPages(graphQL: FakeProfileGraphQL): List<Int> =
    graphQL.requests.mapNotNull { (it.variables?.get("page") as? JsonPrimitive)?.content?.toIntOrNull() }

/**
 * Removing favourites from a category's "View all": optimistic, one request per
 * item, rollback on failure, the profile mirror, and gap-free paging after the
 * server shifts the list up.
 */
class ProfileFavoritesGridRemovalTest {

    @Test
    fun removeIsOptimisticAndSucceeds() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPageOf("anime", false, *favoriteNodes(1, 2))))
        val favourite = FakeFavouriteRepository()
        val holder = gridHolder(graphQL, favourite)

        holder.load(7, ProfileFavoritesCategory.ANIME)
        assertEquals(listOf(1, 2), holder.items.map { it.id })

        holder.removeFavorite(1)

        assertEquals(listOf(2), holder.items.map { it.id }, "removed at once")
        assertFalse(holder.isRemoving(1), "the request already settled")
        assertEquals(listOf<FavouriteTarget>(FavouriteTarget.Anime(1)), favourite.targets)
        assertEquals(1, graphQL.calls, "removing fires no grid request")
    }

    @Test
    fun aFailedRemovalRollsBackIntoItsPosition() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPageOf("anime", false, *favoriteNodes(1, 2, 3))))
        val favourite = FakeFavouriteRepository(result = NetworkResult.Failure(NetworkError.Http(500, null)))
        val holder = gridHolder(graphQL, favourite)

        holder.load(7, ProfileFavoritesCategory.ANIME)
        holder.removeFavorite(2)

        assertEquals(listOf(1, 2, 3), holder.items.map { it.id }, "it reappears where it was")
        assertTrue(holder.actionError != null)
    }

    @Test
    fun aSecondTapWhileInFlightFiresOneRequest() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPageOf("anime", false, *favoriteNodes(1, 2))))
        val favourite = FakeFavouriteRepository().apply { gate = CompletableDeferred() }
        val holder = gridHolder(graphQL, favourite)

        holder.load(7, ProfileFavoritesCategory.ANIME)
        holder.removeFavorite(1)
        holder.removeFavorite(1)

        assertEquals(1, favourite.calls)
        assertTrue(holder.isRemoving(1))
        assertEquals(listOf(2), holder.items.map { it.id })

        favourite.gate!!.complete(Unit)
        assertFalse(holder.isRemoving(1))
        assertEquals(listOf(2), holder.items.map { it.id })
    }

    @Test
    fun aFinishedLoadReArmsTheFooter() {
        val graphQL = FakeProfileGraphQL(
            listOf(
                favouritesPageOf("anime", true, *favoriteNodes(1, 2)),
                favouritesPageOf("anime", false, *favoriteNodes(3)),
            ),
        )
        val holder = gridHolder(graphQL)

        holder.load(7, ProfileFavoritesCategory.ANIME)
        val afterLoad = holder.loadKey
        assertTrue(afterLoad != 0, "a finished load changes the footer key")

        holder.loadMore()
        assertTrue(holder.loadKey != afterLoad, "a finished page also re-arms the footer")
        assertEquals(listOf(1, 2, 3), holder.items.map { it.id })
    }

    @Test
    fun aRemovalPatchesTheProfileMemoryAndDiskWithoutAnExtraRequest() {
        val store = InMemoryFileStore()
        val cache = ProfileDiskCache(store, decodeDispatcher = Dispatchers.Unconfined)
        val graphQL = FakeProfileGraphQL(
            listOf(
                profileJson(anime = listOf(1 to "A", 2 to "B")),
                favouritesPageOf("anime", false, *favoriteNodes(1, 2)),
            ),
        )
        val repository = ProfileRepositoryImpl(graphQL, cache)
        val favourite = FakeFavouriteRepository()
        val profile = ProfileStateHolder(repository, favourite, CoroutineScope(Dispatchers.Unconfined))
        val grid = ProfileFavoritesGridStateHolder(
            repository = repository,
            favouriteRepository = favourite,
            scope = CoroutineScope(Dispatchers.Unconfined),
            onFavoriteRemoved = { category, id -> profile.removeFavoriteLocally(category, id) },
        )

        profile.load(7)
        grid.load(7, ProfileFavoritesCategory.ANIME)
        assertEquals(listOf("A", "B"), profile.favorites.anime.map { it.name })

        grid.removeFavorite(1)

        assertEquals(listOf("B"), profile.favorites.anime.map { it.name })
        runBlocking {
            assertNull(
                cache.read(7)?.favorites?.anime?.firstOrNull { it.id == 1 },
                "the disk cache no longer has it",
            )
        }

        // The profile is fresh: a re-open inside the TTL asks for nothing.
        profile.load(7)
        assertEquals(2, graphQL.calls)
    }

    // ────────────── gap-free paging after removals ──────────────

    @Test
    fun aRemovalRewindsToCoverTheGap() {
        val graphQL = FakeProfileGraphQL(
            listOf(
                favouritesPageOf("anime", true, *favoriteNodes(1, 2, 3)),
                // The server shifted up by 2: the new page 1 starts at 3.
                favouritesPageOf("anime", true, *favoriteNodes(3, 4, 5)),
                favouritesPageOf("anime", false, *favoriteNodes(6, 7)),
            ),
        )
        val holder = gridHolder(graphQL)

        holder.load(7, ProfileFavoritesCategory.ANIME)
        holder.removeFavorite(1)
        holder.removeFavorite(2)
        holder.loadMore() // re-reads page 1
        holder.loadMore() // then page 2

        assertEquals(listOf(3, 4, 5, 6, 7), holder.items.map { it.id }, "no gap after removing 2")
        assertEquals(listOf(1, 1, 2), requestedPages(graphQL))
    }

    @Test
    fun aRemovalOfMoreThanOnePageRewindsFurtherBack() {
        val graphQL = FakeProfileGraphQL(
            listOf(
                favouritesPageOf("anime", true, *rangeNodes(1..25)),
                favouritesPageOf("anime", true, *rangeNodes(26..50)),
                // 26 removals shift everything up by 26: the new page 1 starts at 27.
                favouritesPageOf("anime", true, *rangeNodes(27..51)),
                favouritesPageOf("anime", true, *rangeNodes(52..76)),
                favouritesPageOf("anime", false, *rangeNodes(77..80)),
            ),
        )
        val holder = gridHolder(graphQL)

        holder.load(7, ProfileFavoritesCategory.ANIME)
        holder.loadMore() // page 2: 1..50 loaded
        (1..26).forEach { holder.removeFavorite(it) }
        holder.loadMore() // k = 26 ≥ page size: rewinds one page further (page 1)
        holder.loadMore() // page 2
        holder.loadMore() // page 3

        assertEquals((27..80).toList(), holder.items.map { it.id }, "26 removals leave no gap")
        assertEquals(listOf(1, 2, 1, 2, 3), requestedPages(graphQL))
    }
}
