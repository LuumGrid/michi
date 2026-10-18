package com.luum.michi.app.profile

import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.mediaList.InMemoryFileStore
import com.luum.michi.app.profile.domain.ProfileData
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.domain.model.ProfileStats
import com.luum.michi.app.profile.domain.model.nonEmptyCategories
import com.luum.michi.app.profile.repository.ProfileDiskCache
import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.profile.ui.state.ProfileStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun cachedData(): ProfileData = ProfileData(
    stats = ProfileStats(animeCount = 1, mangaCount = 0, followingCount = 0, followersCount = 0),
    favorites = ProfileFavorites(
        anime = listOf(ProfileFavorite(1, "Cached", CoverSizes(large = "l"), null)),
        manga = emptyList(),
        characters = emptyList(),
        staff = emptyList(),
        studios = emptyList(),
    ),
)

private fun diskCache(store: InMemoryFileStore): ProfileDiskCache =
    ProfileDiskCache(store, decodeDispatcher = Dispatchers.Unconfined)

private fun holder(
    graphQL: FakeProfileGraphQL,
    favourite: FakeFavouriteRepository = FakeFavouriteRepository(),
    cache: ProfileDiskCache? = null,
): ProfileStateHolder = ProfileStateHolder(
    ProfileRepositoryImpl(graphQL, cache),
    favourite,
    CoroutineScope(Dispatchers.Unconfined),
)

/**
 * Removing a favourite from the profile: optimistic, one request, rollback on
 * failure, cache rewritten, and gated by the network-confirmed state.
 */
class ProfileFavoriteRemovalTest {

    @Test
    fun removeIsOptimisticAndRewritesTheCache() {
        val store = InMemoryFileStore()
        val cache = diskCache(store)
        val graphQL = FakeProfileGraphQL(listOf(profileJson(anime = listOf(1 to "A", 2 to "B"))))
        val favourite = FakeFavouriteRepository()
        val holder = holder(graphQL, favourite, cache)

        holder.load(7)
        assertEquals(listOf("A", "B"), holder.favorites.anime.map { it.name })

        holder.removeFavorite(ProfileFavoritesCategory.ANIME, 1)

        assertEquals(listOf("B"), holder.favorites.anime.map { it.name }, "removed at once")
        assertEquals(listOf<FavouriteTarget>(FavouriteTarget.Anime(1)), favourite.targets)
        assertEquals(1, graphQL.calls, "removing fires no profile request")

        runBlocking {
            assertEquals(
                listOf("B"),
                cache.read(7)?.favorites?.anime?.map { it.name },
                "the disk cache was rewritten without the removed favourite",
            )
        }
    }

    @Test
    fun aFailedRemovalRollsBackIntoItsPosition() {
        val graphQL = FakeProfileGraphQL(listOf(profileJson(anime = listOf(1 to "A", 2 to "B", 3 to "C"))))
        val favourite = FakeFavouriteRepository(result = NetworkResult.Failure(NetworkError.Http(500, null)))
        val holder = holder(graphQL, favourite)

        holder.load(7)
        holder.removeFavorite(ProfileFavoritesCategory.ANIME, 2)

        assertEquals(listOf("A", "B", "C"), holder.favorites.anime.map { it.name })
        assertTrue(holder.actionError != null, "the failed removal surfaces a compact error")
    }

    @Test
    fun aSecondTapWhileInFlightFiresOneRequest() {
        val graphQL = FakeProfileGraphQL(listOf(profileJson(anime = listOf(1 to "A", 2 to "B"))))
        val favourite = FakeFavouriteRepository().apply { gate = CompletableDeferred() }
        val holder = holder(graphQL, favourite)

        holder.load(7)
        holder.removeFavorite(ProfileFavoritesCategory.ANIME, 1)
        holder.removeFavorite(ProfileFavoritesCategory.ANIME, 1)

        assertEquals(1, favourite.calls, "one request per removal")
        assertTrue(holder.isRemoving(ProfileFavoritesCategory.ANIME, 1))

        favourite.gate!!.complete(Unit)

        assertEquals(listOf("B"), holder.favorites.anime.map { it.name })
        assertFalse(holder.isRemoving(ProfileFavoritesCategory.ANIME, 1))
    }

    @Test
    fun aCategoryThatEmptiesDisappears() {
        val graphQL = FakeProfileGraphQL(listOf(profileJson(studios = listOf(1 to "Studio"))))
        val holder = holder(graphQL)

        holder.load(7)
        assertTrue(ProfileFavoritesCategory.STUDIOS in holder.favorites.nonEmptyCategories())

        holder.removeFavorite(ProfileFavoritesCategory.STUDIOS, 1)

        assertFalse(ProfileFavoritesCategory.STUDIOS in holder.favorites.nonEmptyCategories())
    }

    @Test
    fun editingIsDisabledWhileTheProfileIsOnlyHydrated() {
        val cache = diskCache(InMemoryFileStore())
        runBlocking { cache.write(7, cachedData()) }

        val gate = CompletableDeferred<Unit>()
        val graphQL = FakeProfileGraphQL(listOf(profileJson(anime = listOf(1 to "A")))).apply { this.gate = gate }
        val holder = holder(graphQL, cache = cache)

        holder.load(7)
        assertTrue(holder.hasContent)
        assertFalse(holder.canEdit, "disk-only data must not be editable")

        gate.complete(Unit)
        assertTrue(holder.canEdit, "the network confirmation unlocks editing")
    }

    @Test
    fun editingSurvivesAFailedRefresh() {
        val page = profileJson(anime = listOf(1 to "A"))
        val graphQL = FakeProfileGraphQL(listOf(page, page), failureAt = 1)
        val holder = holder(graphQL)

        holder.load(7)
        assertTrue(holder.canEdit)

        holder.refresh()

        assertTrue(holder.canEdit, "an already-confirmed profile stays editable")
        assertTrue(holder.error != null, "the failed refresh is the compact load error")
    }
}
