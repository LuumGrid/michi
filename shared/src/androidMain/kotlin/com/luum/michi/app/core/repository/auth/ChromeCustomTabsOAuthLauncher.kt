package com.luum.michi.app.core.repository.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.luum.michi.app.core.domain.auth.AniListOAuthConfig
import com.luum.michi.app.core.domain.auth.AniListOAuthLauncher

/**
 * Opens the AniList OAuth authorize page in a Chrome Custom Tab. After the user
 * authorizes, AniList redirects to `michi://oauth/callback#access_token=...`,
 * which `MainActivity`'s intent-filter (configured in Iteration 3) catches.
 */
internal class ChromeCustomTabsOAuthLauncher(
    private val context: Context,
) : AniListOAuthLauncher {

    override fun open() {
        val intent = CustomTabsIntent.Builder().build()
        intent.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.launchUrl(context, Uri.parse(AniListOAuthConfig.buildAuthorizeUrl()))
    }
}
