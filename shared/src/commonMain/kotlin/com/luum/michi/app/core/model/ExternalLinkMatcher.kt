package com.luum.michi.app.core.model

/**
 * Matches a link URL to a site from the AniList catalog by host, so a person's
 * biography link can borrow the catalog's site name, icon and colour. Pure.
 *
 * Hosts are normalized (case, `www.`/`mobile.`/`m.` prefixes) and the known
 * aliases (twitter↔x, youtu.be↔youtube.com) collapse to one another. A host
 * also matches a catalog host it is a subdomain of (`usuario.fanbox.cc` →
 * `fanbox.cc`), on a dot boundary only (`notfanbox.cc` does not match). When
 * several catalog sites match, the most specific (longest) host wins.
 */
internal fun matchCatalogSite(catalog: List<ExternalLink>, url: String): ExternalLink? {
    val host = canonicalHost(url) ?: return null
    return catalog
        .mapNotNull { candidate ->
            val candidateHost = canonicalHost(candidate.url) ?: return@mapNotNull null
            if (host == candidateHost || host.endsWith(".$candidateHost")) {
                candidate to candidateHost.length
            } else {
                null
            }
        }
        .maxByOrNull { it.second }
        ?.first
}

/** Lowercased host without common subdomain prefixes or known-alias spellings. */
private fun canonicalHost(url: String): String? {
    val scheme = url.substringBefore("://", missingDelimiterValue = "")
    if (!scheme.equals("http", ignoreCase = true) && !scheme.equals("https", ignoreCase = true)) return null
    val host = url.substringAfter("://", "").substringBefore('/').substringBefore('?').lowercase()
    if (host.isEmpty()) return null
    val trimmed = host.removePrefix("www.").removePrefix("mobile.").removePrefix("m.")
    return when (trimmed) {
        "x.com", "twitter.com" -> "twitter.com"
        "youtu.be" -> "youtube.com"
        else -> trimmed
    }
}
