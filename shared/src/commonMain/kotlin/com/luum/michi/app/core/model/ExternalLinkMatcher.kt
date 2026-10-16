package com.luum.michi.app.core.model

import com.luum.michi.app.core.util.canonicalHost

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
            val candidateHost = canonicalHost(candidate.url.withScheme()) ?: return@mapNotNull null
            if (host == candidateHost || host.endsWith(".$candidateHost")) {
                candidate to candidateHost.length
            } else {
                null
            }
        }
        .maxByOrNull { it.second }
        ?.first
}

/**
 * AniList's catalog mixes full URLs (`https://x.com`) with bare hosts
 * (`instagram.com`), which [canonicalHost] would drop; give the latter a scheme.
 */
private fun String.withScheme(): String = if (contains("://")) this else "https://$this"
