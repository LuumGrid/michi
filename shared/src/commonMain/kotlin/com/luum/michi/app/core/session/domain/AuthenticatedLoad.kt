package com.luum.michi.app.core.session.domain

/**
 * Session gate for token-mandatory loads (user lists, account, notifications,
 * settings sync, entry mutations). The future UI calls this instead of raw
 * `load()`s; discover, calendar and media-detail reads load directly without
 * authentication (try-before-login).
 *
 * Takes the already-observed [SessionState] (the UI routes on it anyway) so no
 * extra bootstrap request happens here. Returns whether the load ran.
 */
internal suspend fun loadIfAuthenticated(
    state: SessionState,
    load: suspend (viewerId: Int) -> Unit,
): Boolean = when (state) {
    is SessionState.Authenticated -> {
        load(state.viewer.id)
        true
    }
    else -> false
}
