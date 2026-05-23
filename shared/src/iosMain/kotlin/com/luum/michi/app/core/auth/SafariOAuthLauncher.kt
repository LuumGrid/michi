package com.luum.michi.app.core.auth

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * Opens the AniList OAuth authorize page in the system browser. After the user
 * authorizes, AniList redirects to `michi://oauth/callback#access_token=...`,
 * which the iOS app's URL scheme handler (configured in Iteration 3) catches.
 *
 * `SFSafariViewController` would give a slightly better UX (in-app browser),
 * but it requires a `UIViewController` reference to present on. Plain
 * `openURL` works without that coupling.
 */
internal class SafariOAuthLauncher : AniListOAuthLauncher {
    override fun open() {
        val url = NSURL.URLWithString(AniListOAuthConfig.buildAuthorizeUrl()) ?: return
        UIApplication.sharedApplication.openURL(url)
    }
}
