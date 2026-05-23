package com.luum.michi.app.core.auth

internal sealed class AniListOAuthResult {
    data class Success(val token: AniListToken) : AniListOAuthResult()
    data object Cancelled : AniListOAuthResult()
    data class Failure(val message: String) : AniListOAuthResult()
}
