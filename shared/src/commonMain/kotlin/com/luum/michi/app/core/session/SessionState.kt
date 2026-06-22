package com.luum.michi.app.core.session

import com.luum.michi.app.core.network.NetworkError

internal sealed class SessionState {
    /** Initial state while the app is checking persisted token storage. */
    data object Loading : SessionState()

    /** No valid token found. The auth landing screen should be shown. */
    data object Anonymous : SessionState()

    /** A valid token and viewer were resolved. The shell should be shown. */
    data class Authenticated(val viewer: Viewer) : SessionState()

    /** Token exists but viewer fetch failed (network error, expired token, etc.). */
    data class Error(val error: NetworkError) : SessionState()
}
