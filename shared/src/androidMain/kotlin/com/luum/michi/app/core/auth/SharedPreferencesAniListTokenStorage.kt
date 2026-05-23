package com.luum.michi.app.core.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PrefsName = "michi_anilist_auth"
private const val KeyAccessToken = "access_token"
private const val KeyTokenType = "token_type"
private const val KeyExpiresInSeconds = "expires_in_seconds"
private const val KeySavedAtEpochSeconds = "saved_at_epoch_seconds"

/**
 * Persists the AniList token in `SharedPreferences`. The token is stored in
 * plain text — sufficient for an API that allows the user to revoke tokens at
 * any time from anilist.co/settings/apps. Upgrade to `EncryptedSharedPreferences`
 * if you want defense in depth.
 */
internal class SharedPreferencesAniListTokenStorage(
    context: Context,
) : AniListTokenStorage {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)

    private val state = MutableStateFlow(readToken())

    override val tokenFlow: StateFlow<AniListToken?> = state.asStateFlow()

    override suspend fun save(token: AniListToken) {
        prefs.edit().apply {
            putString(KeyAccessToken, token.accessToken)
            putString(KeyTokenType, token.tokenType)
            token.expiresInSeconds?.let { putLong(KeyExpiresInSeconds, it) }
                ?: remove(KeyExpiresInSeconds)
            putLong(KeySavedAtEpochSeconds, token.savedAtEpochSeconds)
        }.apply()
        state.value = token
    }

    override suspend fun load(): AniListToken? = readToken()

    override suspend fun clear() {
        prefs.edit()
            .remove(KeyAccessToken)
            .remove(KeyTokenType)
            .remove(KeyExpiresInSeconds)
            .remove(KeySavedAtEpochSeconds)
            .apply()
        state.value = null
    }

    private fun readToken(): AniListToken? {
        val accessToken = prefs.getString(KeyAccessToken, null)?.takeIf { it.isNotBlank() }
            ?: return null
        val tokenType = prefs.getString(KeyTokenType, null)?.takeIf { it.isNotBlank() } ?: "Bearer"
        val expiresIn = if (prefs.contains(KeyExpiresInSeconds)) {
            prefs.getLong(KeyExpiresInSeconds, 0L)
        } else {
            null
        }
        val savedAt = prefs.getLong(KeySavedAtEpochSeconds, currentEpochSeconds())
        return AniListToken(
            accessToken = accessToken,
            tokenType = tokenType,
            expiresInSeconds = expiresIn,
            savedAtEpochSeconds = savedAt,
        )
    }
}
