package com.luum.michi.app.core

import com.luum.michi.app.core.auth.domain.AniListToken
import com.luum.michi.app.core.auth.domain.AniListTokenStorage
import com.luum.michi.app.core.auth.domain.InMemoryAniListTokenStorage
import com.luum.michi.app.core.auth.domain.currentEpochSeconds
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.session.domain.AniListViewerRepository
import com.luum.michi.app.core.session.domain.SessionManager
import com.luum.michi.app.core.session.domain.SessionState
import com.luum.michi.app.core.session.domain.Viewer
import kotlinx.coroutines.runBlocking
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
): Pair<SessionManager, FakeViewerRepository> {
    val repository = FakeViewerRepository(viewerResult)
    return SessionManager(storage, repository) to repository
}

class SessionManagerTest {

    @Test
    fun bootstrapWithoutTokenGoesAnonymousWithoutFetching() {
        val storage = InMemoryAniListTokenStorage()
        val (manager, repository) = manager(storage, NetworkResult.Success(viewer()))
        runBlocking { manager.bootstrap() }
        assertTrue(manager.state.value is SessionState.Anonymous)
        assertEquals(0, repository.calls)
    }

    @Test
    fun bootstrapWithExpiredTokenGoesAnonymous() {
        val storage = InMemoryAniListTokenStorage()
        runBlocking { storage.save(expiredToken()) }
        val (manager, repository) = manager(storage, NetworkResult.Success(viewer()))
        runBlocking { manager.bootstrap() }
        assertTrue(manager.state.value is SessionState.Anonymous)
        assertEquals(0, repository.calls)
    }

    @Test
    fun bootstrapWithValidTokenAuthenticates() {
        val storage = InMemoryAniListTokenStorage()
        runBlocking { storage.save(freshToken()) }
        val (manager) = manager(storage, NetworkResult.Success(viewer()))
        runBlocking { manager.bootstrap() }
        val state = manager.state.value
        assertTrue(state is SessionState.Authenticated)
        assertEquals("user", state.viewer.name)
    }

    @Test
    fun bootstrapFailureSurfacesError() {
        val storage = InMemoryAniListTokenStorage()
        runBlocking { storage.save(freshToken()) }
        val error = NetworkError.Http(500, null)
        val (manager) = manager(storage, NetworkResult.Failure(error))
        runBlocking { manager.bootstrap() }
        val state = manager.state.value
        assertTrue(state is SessionState.Error)
        assertEquals(error, state.error)
    }

    @Test
    fun oAuthCallbackSavesAndAuthenticates() {
        val storage = InMemoryAniListTokenStorage()
        val (manager) = manager(storage, NetworkResult.Success(viewer()))
        val token = freshToken()
        runBlocking { manager.onOAuthCallback(token) }
        runBlocking { assertEquals(token, storage.load()) }
        assertTrue(manager.state.value is SessionState.Authenticated)
    }

    @Test
    fun logoutClearsAndGoesAnonymous() {
        val storage = InMemoryAniListTokenStorage()
        runBlocking { storage.save(freshToken()) }
        val (manager) = manager(storage, NetworkResult.Success(viewer()))
        runBlocking {
            manager.bootstrap()
            manager.logout()
            assertNull(storage.load())
        }
        assertTrue(manager.state.value is SessionState.Anonymous)
    }
}
