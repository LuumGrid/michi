package com.luum.michi.app.core.model

/**
 * Matches a link URL to a site from the AniList catalog by host, so a person's
 * biography link can borrow the catalog's site name, icon and colour. Pure.
 *
 * Hosts are normalized (case, `www.`/`mobile.`/`m.` prefixes) and the known
 * aliases (twitter↔x, youtu.be↔youtube.com) collapse to one another.
 */
internal fun matchCatalogSite(catalog: List<ExternalLink>, url: String): ExternalLink? {
    val host = normalizedHost(url) ?: return null
    return catalog.firstOrNull { candidate ->
        val candidateHost = normalizedHost(candidate.url) ?: return@firstOrNull false
        candidateHost == host || aliases(candidateHost) == aliases(host)
    }
}

/** Registrable-ish host: lowercased, without common subdomain prefixes. */
private fun normalizedHost(url: String): String? {
    val scheme = url.substringBefore("://", missingDelimiterValue = "")
    if (!scheme.equals("http", ignoreCase = true) && !scheme.equals("https", ignoreCase = true)) return null
    val host = url.substringAfter("://", "").substringBefore('/').substringBefore('?').lowercase()
    if (host.isEmpty()) return null
    return host.removePrefix("www.").removePrefix("mobile.").removePrefix("m.")
}

/** Collapses known aliases to a canonical host. */
private fun aliases(host: String): String = when (host) {
    "x.com", "twitter.com" -> "twitter.com"
    "youtu.be" -> "youtube.com"
    else -> host
}
