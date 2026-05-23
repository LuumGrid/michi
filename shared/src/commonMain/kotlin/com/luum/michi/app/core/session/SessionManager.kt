package com.luum.michi.app.core.session

import com.luum.michi.app.core.auth.AniListToken
import com.luum.michi.app.core.auth.AniListTokenStorage
import com.luum.michi.app.core.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates the AniList access token storage with the current `Viewer`.
 *
 * Lifecycle:
 *  - `bootstrap()` is called at app start: load the stored token, fetch viewer.
 *  - `onOAuthCallback(token)` is called after the deep link returns from AniList.
 *  - `logout()` clears the token and returns to `Anonymous`.
 *
 * The state is exposed as a `StateFlow<SessionState>` that `App.kt` observes to
 * route between `AuthLandingScreen` and `ShellScreen`.
 */
internal class SessionManager(
    private val tokenStorage: AniListTokenStorage,
    private val viewerRepository: AniListViewerRepository,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    suspend fun bootstrap() {
        val stored = tokenStorage.load()
        if (stored == null || stored.isExpired) {
            _state.value = SessionState.Anonymous
            return
        }
        resolveViewer()
    }

    suspend fun onOAuthCallback(token: AniListToken) {
        tokenStorage.save(token)
        resolveViewer()
    }

    suspend fun logout() {
        tokenStorage.clear()
        _state.value = SessionState.Anonymous
    }

    private suspend fun resolveViewer() {
        when (val result = viewerRepository.fetchViewer()) {
            is NetworkResult.Success -> _state.value = SessionState.Authenticated(result.value)
            is NetworkResult.Failure -> _state.value = SessionState.Error(result.error.toString())
        }
    }
}
