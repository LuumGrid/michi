package com.luum.michi.app.core.util

/**
 * Single shared URL helpers. Before this, the http(s) check was written three
 * times (the opener, the catalog matcher and the biography parser) and the
 * host normalization twice, so the rules could drift.
 */

/** True for a plain `http`/`https` URL (no `javascript:`, no custom scheme). */
internal fun isWebUrl(raw: String?): Boolean {
    if (raw.isNullOrBlank()) return false
    val scheme = raw.substringBefore("://", missingDelimiterValue = "")
    return scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)
}

/**
 * Lowercased host of an http(s) URL, or null when it is not one. Strips the
 * scheme, userinfo, port, path, `?`/`#` and the common `www.`/`mobile.`/`m.`
 * prefixes; any other subdomain is kept (`usuario.fanbox.cc` stays intact).
 */
internal fun urlHost(raw: String?): String? {
    if (!isWebUrl(raw)) return null
    val rest = raw!!
        .substringAfter("://", "")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('@')
        .substringBefore(':')
    val host = rest.lowercase()
        .removePrefix("www.")
        .removePrefix("mobile.")
        .removePrefix("m.")
    return host.ifEmpty { null }
}

/**
 * [urlHost] with the known aliases collapsed (twitter↔x, youtu.be↔youtube.com),
 * so a site can be matched by one spelling.
 */
internal fun canonicalHost(raw: String?): String? {
    val host = urlHost(raw) ?: return null
    return when (host) {
        "x.com", "twitter.com" -> "twitter.com"
        "youtu.be" -> "youtube.com"
        else -> host
    }
}

/** AniList's own host: `anilist.co` or a real subdomain, never `fakeanilist.co`. */
internal fun isAniListHost(host: String): Boolean =
    host == "anilist.co" || host.endsWith(".anilist.co")
