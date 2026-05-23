package com.luum.michi.app.core.auth

internal data class AniListToken(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long? = null,
    val savedAtEpochSeconds: Long,
) {
    val isExpired: Boolean
        get() {
            val expiresIn = expiresInSeconds ?: return false
            val nowSeconds = currentEpochSeconds()
            return nowSeconds >= savedAtEpochSeconds + expiresIn
        }

    val authorizationHeaderValue: String
        get() = "$tokenType $accessToken"
}

internal expect fun currentEpochSeconds(): Long
