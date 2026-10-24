package com.luum.michi.app.profile

import com.luum.michi.app.core.auth.domain.AniListToken
import com.luum.michi.app.core.auth.domain.InMemoryAniListTokenStorage
import com.luum.michi.app.core.auth.domain.currentEpochSeconds
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.session.domain.AniListViewerRepository
import com.luum.michi.app.core.session.domain.CachedViewerStore
import com.luum.michi.app.core.session.domain.SessionManager
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.mediaList.InMemoryFileStore
import com.luum.michi.app.profile.domain.ProfileData
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.domain.model.ProfileMediaTypeStats
import com.luum.michi.app.profile.domain.model.ProfileStatDistributionEntry
import com.luum.michi.app.profile.domain.model.ProfileStats
import com.luum.michi.app.profile.repository.ProfileDiskCache
import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.profile.repository.profileCacheFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun diskCache(store: InMemoryFileStore): ProfileDiskCache =
    ProfileDiskCache(store, decodeDispatcher = Dispatchers.Unconfined)

/** A full profile (counts, distributions and every favourites bucket). */
private fun richProfile(): ProfileData = ProfileData(
    stats = ProfileStats(
        animeCount = 10,
        mangaCount = 5,
        followingCount = 9,
        followersCount = 7,
        anime = ProfileMediaTypeStats(
            count = 10,
            episodesOrChapters = 120,
            daysOrVolumes = 5.5,
            meanScore = 78.0,
            standardDeviation = 4.2,
            scoreDistribution = listOf(ProfileStatDistributionEntry("80", 3)),
            formatDistribution = listOf(ProfileStatDistributionEntry("TV", 4)),
            statusDistribution = listOf(ProfileStatDistributionEntry("CURRENT", 2)),
            topGenres = listOf(ProfileStatDistributionEntry("Action", 6)),
        ),
    ),
    favorites = ProfileFavorites(
        anime = listOf(
            ProfileFavorite(
                id = 1,
                name = "A",
                image = CoverSizes(medium = "m", large = "l", extraLarge = "xl"),
                paletteHex = "#123456",
            ),
        ),
        manga = listOf(ProfileFavorite(2, "M", CoverSizes(large = "l2"), null)),
        characters = listOf(ProfileFavorite(3, "C", CoverSizes(large = "l3"), null)),
        staff = listOf(ProfileFavorite(4, "S", null, null)),
        studios = listOf(ProfileFavorite(5, "Studio", null, null)),
    ),
)

private fun viewer() = Viewer(id = 1, name = "user")

private fun freshToken() = AniListToken(
    accessToken = "abc",
    savedAtEpochSeconds = currentEpochSeconds(),
    expiresInSeconds = 3600L,
)

private class FakeViewerRepo(private val result: NetworkResult<Viewer>) : AniListViewerRepository {
    override suspend fun fetchViewer(): NetworkResult<Viewer> = result
}

private class FakeCachedViewerStore : CachedViewerStore {
    override fun load(): Viewer? = null
    override fun save(viewer: Viewer) {}
    override fun clear() {}
}

class ProfileCacheTest {

    @Test
    fun aWrittenProfileReadsBackWhole() {
        val store = InMemoryFileStore()
        val cache = diskCache(store)

        runBlocking {
            cache.write(7, richProfile())
            assertEquals(richProfile(), cache.read(7))
        }
    }

    @Test
    fun aFavoriteSubtitleSurvivesTheDiskRoundTrip() {
        val store = InMemoryFileStore()
        val cache = diskCache(store)
        val profile = richProfile().let { data ->
            data.copy(
                favorites = data.favorites.copy(
                    anime = listOf(
                        ProfileFavorite(
                            id = 1,
                            name = "A",
                            image = CoverSizes(large = "l"),
                            paletteHex = "#123456",
                            subtitle = "TV · 2013",
                        ),
                    ),
                ),
            )
        }

        runBlocking {
            cache.write(7, profile)
            assertEquals("TV · 2013", cache.read(7)?.favorites?.anime?.single()?.subtitle)
        }
    }

    @Test
    fun theCacheFileNameCarriesTheCurrentFormatVersion() {
        assertEquals("profile_v2_7.json", profileCacheFileName(7))
    }

    @Test
    fun theUnreadNotificationCountIsNotPersisted() {
        val store = InMemoryFileStore()
        val cache = diskCache(store)

        runBlocking {
            cache.write(7, richProfile().copy(unreadNotificationCount = 5))
            assertNull(
                cache.read(7)?.unreadNotificationCount,
                "the dot's count is live-only: a stale one would lie on a cold start",
            )
        }
    }

    @Test
    fun anotherUsersProfileFileIsNeverRead() {
        val store = InMemoryFileStore()
        val cache = diskCache(store)

        runBlocking {
            cache.write(1, richProfile())
            assertNull(cache.read(2), "user 2 must not read user 1's file")
        }
    }

    @Test
    fun clearRemovesEveryProfileFile() {
        val store = InMemoryFileStore()
        val cache = diskCache(store)

        runBlocking {
            cache.write(1, richProfile())
            cache.clear()
            assertNull(cache.read(1))
        }
    }

    @Test
    fun aCorruptFileIsIgnoredAndDeleted() {
        val store = InMemoryFileStore()
        val cache = diskCache(store)
        store.put(profileCacheFileName(7), "{ not json")

        runBlocking {
            assertNull(cache.read(7))
        }
        assertFalse(store.contains(profileCacheFileName(7)), "the corrupt file is deleted")
    }

    @Test
    fun aSuccessfulNetworkLoadIsPersistedAndReadable() {
        val store = InMemoryFileStore()
        val cache = diskCache(store)
        val repository = ProfileRepositoryImpl(FakeProfileGraphQL(listOf(profileData())), cache)

        runBlocking {
            assertTrue(repository.loadProfile(1) is NetworkResult.Success)
        }
        assertTrue(store.contains(profileCacheFileName(1)), "the load is persisted")

        runBlocking {
            val cached = repository.loadCachedProfile(1)
            assertEquals(10, cached?.stats?.animeCount)
            assertEquals("A", cached?.favorites?.anime?.single()?.name)
            assertNull(repository.loadCachedProfile(2), "another user has no file")
        }
    }

    @Test
    fun logoutDeletesTheProfileFile() {
        val store = InMemoryFileStore()
        val cache = diskCache(store)
        val storage = InMemoryAniListTokenStorage()

        runBlocking {
            storage.save(freshToken())
            cache.write(1, richProfile())
            val manager = SessionManager(
                tokenStorage = storage,
                viewerRepository = FakeViewerRepo(NetworkResult.Success(viewer())),
                cachedViewerStore = FakeCachedViewerStore(),
                onClearUserData = { cache.clear() },
            )
            manager.bootstrap()
            manager.logout()
        }

        assertFalse(store.contains(profileCacheFileName(1)))
        assertTrue(store.clearCount > 0)
    }

    @Test
    fun theGridHolderStillPagesThroughTheRepository() {
        val graphQL = FakeProfileGraphQL(listOf(favouritesPage("studios", "Studio", false)))
        val repository = ProfileRepositoryImpl(graphQL, diskCache(InMemoryFileStore()))

        runBlocking {
            val result = repository.loadFavoritesPage(1, ProfileFavoritesCategory.STUDIOS, page = 1)
            assertTrue(result is NetworkResult.Success)
            assertEquals("Studio", result.value.items.single().name)
        }
    }
}
