package com.luum.michi.app.profile

import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.mediaList.InMemoryFileStore
import com.luum.michi.app.profile.domain.ProfileData
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileStats
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

private fun cachedProfile(): ProfileData = ProfileData(
    stats = ProfileStats(
        animeCount = 10,
        mangaCount = 5,
        followingCount = 9,
        followersCount = 7,
    ),
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

/**
 * The profile paints its disk cache first and then revalidates against the
 * network, exactly like the saved lists: the file is never fresh, but the TTL
 * still dedups a re-open after a successful load.
 */
class ProfileHolderCacheTest {

    @Test
    fun coldStartPaintsTheCacheThenRevalidates() {
        val cache = diskCache(InMemoryFileStore())
        runBlocking { cache.write(7, cachedProfile()) }

        val gate = CompletableDeferred<Unit>()
        val graphQL = FakeProfileGraphQL(listOf(profileData(animeCount = 99))).apply { this.gate = gate }
        val holder = ProfileStateHolder(ProfileRepositoryImpl(graphQL, cache), CoroutineScope(Dispatchers.Unconfined))

        holder.load(7)

        assertEquals(1, graphQL.calls, "the network revalidation was launched")
        assertEquals(10, holder.stats.animeCount, "the cached profile is shown first")
        assertEquals("Cached", holder.favorites.anime.single().name)
        assertTrue(holder.hasContent)
        assertFalse(holder.isLoading, "hydrated data turns the full-page spinner off")

        gate.complete(Unit)

        assertEquals(99, holder.stats.animeCount, "the network replaces the cached profile")
    }

    @Test
    fun aFailedRevalidationKeepsTheCachedProfile() {
        val cache = diskCache(InMemoryFileStore())
        runBlocking { cache.write(7, cachedProfile()) }

        val graphQL = FakeProfileGraphQL(listOf(profileData()), failureAt = 0)
        val holder = ProfileStateHolder(ProfileRepositoryImpl(graphQL, cache), CoroutineScope(Dispatchers.Unconfined))

        holder.load(7)

        assertEquals(10, holder.stats.animeCount, "the cached profile stays on screen")
        assertTrue(holder.hasContent)
        assertTrue(holder.error != null, "the failed revalidation surfaces the compact error")
    }

    @Test
    fun aSuccessfulLoadMakesTheNextOpenFreeWithinTheTtl() {
        val graphQL = FakeProfileGraphQL(listOf(profileData(animeCount = 10)))
        val holder = ProfileStateHolder(
            ProfileRepositoryImpl(graphQL, diskCache(InMemoryFileStore())),
            CoroutineScope(Dispatchers.Unconfined),
        )

        holder.load(1)
        assertEquals(10, holder.stats.animeCount)

        holder.load(1)

        assertEquals(1, graphQL.calls, "the TTL shortcuts a re-open within 5 minutes")
        assertFalse(holder.isLoading)
    }

    @Test
    fun anotherUsersCacheIsNeverShown() {
        val cache = diskCache(InMemoryFileStore())
        runBlocking { cache.write(1, cachedProfile()) }

        val gate = CompletableDeferred<Unit>()
        val graphQL = FakeProfileGraphQL(listOf(profileData(animeCount = 55))).apply { this.gate = gate }
        val holder = ProfileStateHolder(ProfileRepositoryImpl(graphQL, cache), CoroutineScope(Dispatchers.Unconfined))

        holder.load(2)

        assertEquals(0, holder.stats.animeCount, "user 1's cached profile is never shown for user 2")

        gate.complete(Unit)

        assertEquals(55, holder.stats.animeCount)
    }
}
