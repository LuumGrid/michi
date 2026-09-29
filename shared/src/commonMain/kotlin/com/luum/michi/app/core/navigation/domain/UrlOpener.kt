package com.luum.michi.app.core.navigation.domain

/**
 * Opens an external URL in the platform browser (plain `ACTION_VIEW` on
 * Android; not wired on iOS yet). Callers guard with [isWebUrl] so only
 * http(s) links ever reach the platform (no `javascript:` or custom
 * schemes, no crash on malformed input).
 */
internal interface UrlOpener {
    fun open(url: String)
}

/** No-op opener for environments without a browser (tests, previews, iOS). */
internal class NoopUrlOpener : UrlOpener {
    override fun open(url: String) = Unit
}

internal fun isWebUrl(raw: String?): Boolean {
    if (raw.isNullOrBlank()) return false
    val scheme = raw.substringBefore("://", missingDelimiterValue = "")
    return scheme.equals("http", ignoreCase = true) ||
        scheme.equals("https", ignoreCase = true)
}
