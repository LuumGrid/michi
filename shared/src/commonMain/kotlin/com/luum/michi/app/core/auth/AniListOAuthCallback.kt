package com.luum.michi.app.core.auth

/**
 * Parses the URL fragment returned by AniList's implicit grant flow.
 *
 * Expected format:
 * `michi://oauth/callback#access_token=eyJ...&token_type=Bearer&expires_in=31536000`
 *
 * Returns `null` if the URI is not a valid AniList OAuth callback (wrong scheme,
 * missing fragment, or missing access_token).
 */
internal fun parseAniListOAuthCallback(uri: String): AniListToken? {
    if (!uri.startsWith(AniListOAuthConfig.RedirectUri)) return null

    val fragmentIndex = uri.indexOf('#')
    if (fragmentIndex == -1 || fragmentIndex == uri.lastIndex) return null
    val fragment = uri.substring(fragmentIndex + 1)
    if (fragment.isBlank()) return null

    val params = fragment.split('&')
        .mapNotNull { entry ->
            val eqIndex = entry.indexOf('=')
            if (eqIndex == -1) return@mapNotNull null
            val key = entry.substring(0, eqIndex)
            val value = entry.substring(eqIndex + 1)
            key to value
        }
        .toMap()

    val accessToken = params["access_token"]?.takeIf { it.isNotBlank() } ?: return null
    val tokenType = params["token_type"]?.takeIf { it.isNotBlank() } ?: "Bearer"
    val expiresIn = params["expires_in"]?.toLongOrNull()

    return AniListToken(
        accessToken = accessToken,
        tokenType = tokenType,
        expiresInSeconds = expiresIn,
        savedAtEpochSeconds = currentEpochSeconds(),
    )
}
