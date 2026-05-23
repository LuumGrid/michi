package com.luum.michi.app.core.auth

import com.luum.michi.app.core.network.AniListEndpoints

/**
 * OAuth client configuration for AniList. The `ClientId` is generated from
 * `local.properties` at build time (see `AniListBuildConfig`) so it is not
 * committed to source control.
 *
 * The `RedirectUri` must match exactly the redirect URI registered with the
 * AniList client at https://anilist.co/settings/developer.
 */
internal object AniListOAuthConfig {
    val ClientId: String = AniListBuildConfig.ClientId
    const val RedirectUri = "michi://oauth/callback"

    /**
     * AniList implicit grant authorize URL. AniList does not take `redirect_uri`
     * as a query parameter — it uses the one registered with the client_id.
     */
    fun buildAuthorizeUrl(): String =
        "${AniListEndpoints.OAuthAuthorize}?client_id=$ClientId&response_type=token"

    internal val isConfigured: Boolean
        get() = ClientId.isNotBlank()
}
