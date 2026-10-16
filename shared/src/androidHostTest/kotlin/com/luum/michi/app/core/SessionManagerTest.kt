package com.luum.michi.app.core

import com.luum.michi.app.core.auth.domain.AniListToken
import com.luum.michi.app.core.auth.domain.AniListTokenStorage
import com.luum.michi.app.core.auth.domain.InMemoryAniListTokenStorage
import com.luum.michi.app.core.auth.domain.currentEpochSeconds
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.session.domain.AniListViewerRepository
import com.luum.michi.app.core.session.domain.CachedViewerStore
import com.luum.michi.app.core.session.domain.SessionManager
import com.luum.michi.app.core.session.domain.SessionState
import com.luum.michi.app.core.session.domain.Viewer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeViewerRepository(
    private val result: NetworkResult<Viewer>,
) : AniListViewerRepository {
    var calls = 0

    override suspend fun fetchViewer(): NetworkResult<Viewer> {
        calls++
        return result
    }
}

private class InMemoryCachedViewerStore(initial: Viewer? = null) : CachedViewerStore {
    var stored: Viewer? = initial
        private set

    override fun load(): Viewer? = stored
    override fun save(viewer: Viewer) {
        stored = viewer
    }

    override fun clear() {
        stored = null
    }
}

private fun viewer() = Viewer(id = 1, name = "user")

private fun freshToken() = AniListToken(
    accessToken = "abc",
    savedAtEpochSeconds = currentEpochSeconds(),
    expiresInSeconds = 3600L,
)

private fun expiredToken() = AniListToken(
    accessToken = "abc",
    savedAtEpochSeconds = 1L,
    expiresInSeconds = 1L,
)

private fun manager(
    storage: AniListTokenStorage,
    viewerResult: NetworkResult<Viewer>,
    cached: Viewer? = null,
): Triple<SessionManager, FakeViewerRepository, InMemoryCachedViewerStore> {
    val repository = FakeViewerRepository(viewerResult)
    val cache = InMemoryCachedViewerStore(cached)
    return Triple(SessionManager(storage, repository, cache), repository, cache)
}

class SessionManagerTest {

    @Test
    fun bootstrapWithoutTokenGoesAnonymousWithoutFetching() {
        val storage = InMemoryAniListTokenStorage()
        val (manager, repository, _) = manager(storage, NetworkResult.Success(viewer()))
        runBlocking { manager.bootstrap() }
        assertTrue(manager.state.value is SessionState.Anonymous)
        assertEquals(0, repository.calls)
    }

    @Test
    fun bootstrapWithExpiredTokenGoesAnonymous() {
        val storage = InMemoryAniListTokenStorage()
        runBlocking { storage.save(expiredToken()) }
        val (manager, repository, _) = manager(storage, NetworkResult.Success(viewer()))
        runBlocking { manager.bootstrap() }
        assertTrue(manager.state.value is SessionState.Anonymous)
        assertEquals(0, repository.calls)
    }

    @Test
    fun bootstrapWithValidTokenAuthenticates() {
        val storage = InMemoryAniListTokenStorage()
        runBlocking { storage.save(freshToken()) }
        val (manager, _, _) = manager(storage, NetworkResult.Success(viewer()))
        runBlocking { manager.bootstrap() }
        val state = manager.state.value
        assertTrue(state is SessionState.Authenticated)
        assertEquals("user", state.viewer.name)
    }

    @Test
    fun bootstrapFailureSurfacesErrorWithoutCache() {
        val storage = InMemoryAniListTokenStorage()
        runBlocking { storage.save(freshToken()) }
        val error = NetworkError.Http(500, null)
        val (manager, _, _) = manager(storage, NetworkResult.Failure(error))
        runBlocking { manager.bootstrap() }
        val state = manager.state.value
        assertTrue(state is SessionState.Error)
        assertEquals(error, state.error)
    }

    @Test
    fun cachedViewerEntersTheShellAndRefreshes() {
        val storage = InMemoryAniListTokenStorage()
        runBlocking { storage.save(freshToken()) }
        val (manager, _, cache) = manager(
            storage,
            NetworkResult.Success(Viewer(id = 1, name = "fresh")),
            cached = Viewer(id = 1, name = "cached"),
        )

        runBlocking { manager.bootstrap() }

        val state = manager.state.value
        assertTrue(state is SessionState.Authenticated)
        assertEquals("fresh", state.viewer.name)
        assertEquals("fresh", cache.stored?.name, "the fresh viewer is cached")
    }

    @Test
    fun cachedViewerSurvivesANetworkError() {
        val storage = InMemoryAniListTokenStorage()
        runBlocking { storage.save(freshToken()) }
        val (manager, _, cache) = manager(
            storage,
            NetworkResult.Failure(NetworkError.Http(500, null)),
            cached = Viewer(id = 1, name = "cached"),
        )

        runBlocking { manager.bootstrap() }

        val state = manager.state.value
        assertTrue(state is SessionState.Authenticated, "keeps the cached shell, not Error")
        assertEquals("cached", state.viewer.name)
        assertEquals("cached", cache.stored?.name)
    }

    @Test
    fun unauthorizedClearsTokenAndCache() {
        val storage = InMemoryAniListTokenStorage()
        runBlocking { storage.save(freshToken()) }
        val (manager, _, cache) = manager(
            storage,
            NetworkResult.Failure(NetworkError.Unauthorized(null)),
            cached = Viewer(id = 1, name = "cached"),
        )

        runBlocking { manager.bootstrap() }

        assertTrue(manager.state.value is SessionState.Anonymous)
        runBlocking { assertNull(storage.load()) }
        assertNull(cache.stored)
    }

    @Test
    fun oAuthCallbackSavesAndAuthenticates() {
        val storage = InMemoryAniListTokenStorage()
        val (manager, _, cache) = manager(storage, NetworkResult.Success(viewer()))
        val token = freshToken()
        runBlocking { manager.onOAuthCallback(token) }
        runBlocking { assertEquals(token, storage.load()) }
        assertTrue(manager.state.value is SessionState.Authenticated)
        assertEquals("user", cache.stored?.name)
    }

    @Test
    fun logoutClearsTokenAndCache() {
        val storage = InMemoryAniListTokenStorage()
        runBlocking { storage.save(freshToken()) }
        val (manager, _, cache) = manager(storage, NetworkResult.Success(viewer()))
        runBlocking {
            manager.bootstrap()
            manager.logout()
            assertNull(storage.load())
        }
        assertNull(cache.stored)
        assertTrue(manager.state.value is SessionState.Anonymous)
    }

    @Test
    fun onUnauthorizedMidSessionGoesAnonymous() {
        val storage = InMemoryAniListTokenStorage()
        val token = freshToken()
        runBlocking { storage.save(token) }
        val (manager, _, cache) = manager(storage, NetworkResult.Success(viewer()), cached = viewer())
        runBlocking { manager.bootstrap() }
        assertTrue(manager.state.value is SessionState.Authenticated)

        runBlocking { manager.onUnauthorized(token) }

        assertTrue(manager.state.value is SessionState.Anonymous)
        runBlocking { assertNull(storage.load()) }
        assertNull(cache.stored)
    }

    @Test
    fun aStaleUnauthorizedDoesNotCloseANewerSession() {
        val storage = InMemoryAniListTokenStorage()
        val old = freshToken()
        runBlocking { storage.save(old) }
        val (manager, _, _) = manager(storage, NetworkResult.Success(viewer()), cached = viewer())
        runBlocking { manager.bootstrap() }

        // A new token replaced the old one (e.g. re-login) before the 401 lands.
        val fresh = AniListToken(
            accessToken = "new-token",
            savedAtEpochSeconds = currentEpochSeconds(),
            expiresInSeconds = 3600L,
        )
        runBlocking { storage.save(fresh) }

        runBlocking { manager.onUnauthorized(old) }

        assertEquals(fresh, runBlocking { storage.load() }, "the stale 401 is ignored")
    }

    @Test
    fun aBootstrapLandingAfterLogoutCannotResurrectTheSession() = runBlocking {
        val storage = InMemoryAniListTokenStorage()
        storage.save(freshToken())
        val gate = CompletableDeferred<NetworkResult<Viewer>>()
        val slowRepository = object : AniListViewerRepository {
            override suspend fun fetchViewer(): NetworkResult<Viewer> = gate.await()
        }
        val manager = SessionManager(storage, slowRepository, InMemoryCachedViewerStore())

        val bootstrap = launch { manager.bootstrap() }
        yield() // bootstrap enters its viewer refresh and waits at the gate
        val logout = launch { manager.logout() }
        yield() // logout queues behind the serialized bootstrap
        gate.complete(NetworkResult.Success(viewer()))
        bootstrap.join()
        logout.join()

        assertTrue(manager.state.value is SessionState.Anonymous)
    }
}
