package com.luum.michi.app.core.navigation.domain

/**
 * Opens an external URL in the platform browser (plain `ACTION_VIEW` on
 * Android; not wired on iOS yet). Callers guard with `core.util.isWebUrl` so
 * only http(s) links ever reach the platform (no `javascript:` or custom
 * schemes, no crash on malformed input).
 */
internal interface UrlOpener {
    fun open(url: String)
}
