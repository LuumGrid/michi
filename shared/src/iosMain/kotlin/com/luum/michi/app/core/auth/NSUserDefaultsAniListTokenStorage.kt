package com.luum.michi.app.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

private const val KeyAccessToken = "michi_anilist_access_token"
private const val KeyTokenType = "michi_anilist_token_type"
private const val KeyExpiresInSeconds = "michi_anilist_expires_in_seconds"
private const val KeySavedAtEpochSeconds = "michi_anilist_saved_at_epoch_seconds"

/**
 * Persists the AniList token in `NSUserDefaults`. Upgrade to Keychain if you
 * want defense in depth.
 */
internal class NSUserDefaultsAniListTokenStorage(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : AniListTokenStorage {

    private val state = MutableStateFlow(readToken())

    override val tokenFlow: StateFlow<AniListToken?> = state.asStateFlow()

    override suspend fun save(token: AniListToken) {
        defaults.setObject(token.accessToken, KeyAccessToken)
        defaults.setObject(token.tokenType, KeyTokenType)
        if (token.expiresInSeconds != null) {
            defaults.setObject(token.expiresInSeconds.toString(), KeyExpiresInSeconds)
        } else {
            defaults.removeObjectForKey(KeyExpiresInSeconds)
        }
        defaults.setObject(token.savedAtEpochSeconds.toString(), KeySavedAtEpochSeconds)
        state.value = token
    }

    override suspend fun load(): AniListToken? = readToken()

    override suspend fun clear() {
        defaults.removeObjectForKey(KeyAccessToken)
        defaults.removeObjectForKey(KeyTokenType)
        defaults.removeObjectForKey(KeyExpiresInSeconds)
        defaults.removeObjectForKey(KeySavedAtEpochSeconds)
        state.value = null
    }

    private fun readToken(): AniListToken? {
        val accessToken = defaults.stringForKey(KeyAccessToken)?.takeIf { it.isNotBlank() }
            ?: return null
        val tokenType = defaults.stringForKey(KeyTokenType)?.takeIf { it.isNotBlank() } ?: "Bearer"
        val expiresIn = defaults.stringForKey(KeyExpiresInSeconds)?.toLongOrNull()
        val savedAt = defaults.stringForKey(KeySavedAtEpochSeconds)?.toLongOrNull()
            ?: currentEpochSeconds()
        return AniListToken(
            accessToken = accessToken,
            tokenType = tokenType,
            expiresInSeconds = expiresIn,
            savedAtEpochSeconds = savedAt,
        )
    }
}
