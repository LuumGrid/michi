package com.luum.michi.app.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the AniList access token across app launches. Real platform-backed
 * implementations (SharedPreferences on Android, NSUserDefaults on iOS) are
 * wired in Iteration 2. For now `InMemoryAniListTokenStorage` keeps the token
 * only in memory.
 */
internal interface AniListTokenStorage {
    val tokenFlow: StateFlow<AniListToken?>
    suspend fun save(token: AniListToken)
    suspend fun load(): AniListToken?
    suspend fun clear()
}

internal class InMemoryAniListTokenStorage : AniListTokenStorage {
    private val state = MutableStateFlow<AniListToken?>(null)
    override val tokenFlow: StateFlow<AniListToken?> = state.asStateFlow()

    override suspend fun save(token: AniListToken) {
        state.value = token
    }

    override suspend fun load(): AniListToken? = state.value

    override suspend fun clear() {
        state.value = null
    }
}
