package com.luum.michi.app.core.session.domain

import com.luum.michi.app.core.auth.domain.AniListToken
import com.luum.michi.app.core.auth.domain.AniListTokenStorage
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates the AniList access token storage with the current `Viewer`.
 *
 * Lifecycle:
 *  - `bootstrap()` is called at app start: load the stored token, then either
 *    emit the cached viewer immediately (and refresh in the background) or
 *    fetch it.
 *  - `onOAuthCallback(token)` is called after the deep link returns from AniList.
 *  - `logout()` clears the token + cache and returns to `Anonymous`.
 *
 * The state is exposed as a `StateFlow<SessionState>` that `App.kt` observes to
 * route between `AuthLandingScreen` and `Root`.
 */
internal class SessionManager(
    private val tokenStorage: AniListTokenStorage,
    private val viewerRepository: AniListViewerRepository,
    private val cachedViewerStore: CachedViewerStore,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    suspend fun bootstrap() {
        val stored = tokenStorage.load()
        if (stored == null || stored.isExpired) {
            cachedViewerStore.clear()
            _state.value = SessionState.Anonymous
            return
        }
        val cached = cachedViewerStore.load()
        if (cached != null) {
            // Enter the shell immediately; refresh behind the scenes. A network
            // failure keeps the cached viewer instead of flashing an error.
            _state.value = SessionState.Authenticated(cached)
            refreshViewer(keepCachedOnNetworkError = true)
        } else {
            refreshViewer(keepCachedOnNetworkError = false)
        }
    }

    suspend fun onOAuthCallback(token: AniListToken) {
        tokenStorage.save(token)
        refreshViewer(keepCachedOnNetworkError = false)
    }

    suspend fun logout() {
        tokenStorage.clear()
        cachedViewerStore.clear()
        _state.value = SessionState.Anonymous
    }

    private suspend fun refreshViewer(keepCachedOnNetworkError: Boolean) {
        when (val result = viewerRepository.fetchViewer()) {
            is NetworkResult.Success -> {
                cachedViewerStore.save(result.value)
                _state.value = SessionState.Authenticated(result.value)
            }
            is NetworkResult.Failure -> when (result.error) {
                is NetworkError.Unauthorized -> {
                    // Token is no longer valid: drop it and the cached viewer.
                    tokenStorage.clear()
                    cachedViewerStore.clear()
                    _state.value = SessionState.Anonymous
                }
                else -> if (!keepCachedOnNetworkError) {
                    _state.value = SessionState.Error(result.error)
                }
            }
        }
    }
}
