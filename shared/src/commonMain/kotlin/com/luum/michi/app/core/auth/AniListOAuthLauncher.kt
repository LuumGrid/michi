package com.luum.michi.app.core.auth

/**
 * Opens AniList's OAuth authorize page in a browser-like surface (Chrome Custom
 * Tabs on Android, SFSafariViewController/ASWebAuthenticationSession on iOS).
 *
 * The flow does NOT return synchronously — AniList redirects the user back to
 * the app via the `michi://oauth/callback` deep link. The MainActivity
 * (Android) / SceneDelegate (iOS) is responsible for parsing the callback URI
 * and notifying `SessionManager`.
 *
 * Real platform-backed implementations are wired in Iteration 2.
 */
internal interface AniListOAuthLauncher {
    fun open()
}

/** No-op launcher for environments without a real browser (tests, previews). */
internal class NoopAniListOAuthLauncher : AniListOAuthLauncher {
    override fun open() = Unit
}
