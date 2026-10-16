package com.luum.michi.app.core.navigation.domain

/**
 * Opens the platform share sheet with plain text (Android `ACTION_SEND`;
 * not wired on iOS yet). Callers pass already-composed text.
 */
internal interface ShareLauncher {
    fun share(text: String, subject: String? = null)
}
