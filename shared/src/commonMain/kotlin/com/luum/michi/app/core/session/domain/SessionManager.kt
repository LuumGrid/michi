package com.luum.michi.app.core.session.domain

import com.luum.michi.app.core.auth.domain.AniListToken
import com.luum.michi.app.core.auth.domain.AniListTokenStorage
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates the AniList access token storage with the current `Viewer`.
 *
 * Lifecycle:
 *  - `bootstrap()` is called at app start: load the stored token, then either
 *    emit the cached viewer immediately (and refresh in the background) or
 *    fetch it.
 *  - `onOAuthCallback(token)` is called after the deep link returns from AniList.
 *  - `logout()` clears the token + cache and returns to `Anonymous`.
 *  - `onUnauthorized(token)` is called when a request mid-session gets a 401:
 *    it closes the session only if that token is still the stored one.
 *
 * Lifecycle ops are serialized by a [Mutex] and tagged with a generation. The
 * lock is held only for state changes — the `fetchViewer` round-trip runs
 * outside it — so logout/OAuth/401 never wait on the network. A write is
 * dropped when a newer op bumped the generation (e.g. a slow bootstrap landing
 * after logout can never resurrect a stale session).
 *
 * The state is exposed as a `StateFlow<SessionState>` that `App.kt` observes to
 * route between `AuthLandingScreen` and `Root`.
 *
 * [onClearUserData] wipes user-scoped caches (the saved lists) at every point
 * the cached viewer is dropped, so a logout/401 never leaves another user's
 * data on disk.
 */
internal class SessionManager(
    private val tokenStorage: AniListTokenStorage,
    private val viewerRepository: AniListViewerRepository,
    private val cachedViewerStore: CachedViewerStore,
    private val onClearUserData: suspend () -> Unit = {},
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val mutex = Mutex()
    private var generation = 0

    private data class BootstrapEntry(val generation: Int, val cached: Viewer?)

    suspend fun bootstrap() {
        val entry = mutex.withLock {
            val gen = ++generation
            val stored = tokenStorage.load()
            if (stored == null || stored.isExpired) {
                clearUserData()
                _state.value = SessionState.Anonymous
                return
            }
            val cached = cachedViewerStore.load()
            if (cached != null) {
                // Enter the shell immediately; refresh behind the scenes.
                _state.value = SessionState.Authenticated(cached)
            }
            BootstrapEntry(gen, cached)
        }
        // Network outside the lock, so a logout during it does not wait.
        refreshViewer(entry.generation, keepCachedOnNetworkError = entry.cached != null)
    }

    suspend fun onOAuthCallback(token: AniListToken) {
        val gen = mutex.withLock {
            val g = ++generation
            tokenStorage.save(token)
            g
        }
        refreshViewer(gen, keepCachedOnNetworkError = false)
    }

    suspend fun logout() = mutex.withLock {
        generation++
        tokenStorage.clear()
        clearUserData()
        _state.value = SessionState.Anonymous
    }

    /**
     * A mid-session 401: drop the token + cache and go anonymous — but only if
     * [token] is still the stored one, so a stale 401 can never close a newer
     * session.
     */
    suspend fun onUnauthorized(token: AniListToken) = mutex.withLock {
        if (tokenStorage.load()?.accessToken != token.accessToken) return@withLock
        generation++
        tokenStorage.clear()
        clearUserData()
        _state.value = SessionState.Anonymous
    }

    /** Drops the cached viewer and every user-scoped file (lists) together. */
    private suspend fun clearUserData() {
        cachedViewerStore.clear()
        onClearUserData()
    }

    private suspend fun refreshViewer(gen: Int, keepCachedOnNetworkError: Boolean) {
        val result = viewerRepository.fetchViewer()
        mutex.withLock {
            if (gen != generation) return@withLock
            when (result) {
                is NetworkResult.Success -> {
                    cachedViewerStore.save(result.value)
                    _state.value = SessionState.Authenticated(result.value)
                }
                is NetworkResult.Failure -> when (result.error) {
                    is NetworkError.Unauthorized -> {
                        // Token is no longer valid: drop it and the cached viewer.
                        tokenStorage.clear()
                        clearUserData()
                        _state.value = SessionState.Anonymous
                    }
                    else -> if (!keepCachedOnNetworkError) {
                        _state.value = SessionState.Error(result.error)
                    }
                }
            }
        }
    }
}
