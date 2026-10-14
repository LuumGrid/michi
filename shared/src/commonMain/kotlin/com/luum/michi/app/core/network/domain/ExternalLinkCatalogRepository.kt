package com.luum.michi.app.core.network.domain

import com.luum.michi.app.core.model.ExternalLink
import com.luum.michi.app.core.model.matchCatalogSite

/**
 * The AniList catalog of external-link sites (name, url, icon, colour). Used to
 * resolve a person's biography links (which only carry a URL) into the same
 * shape media links already have. Cached in memory + disk (long TTL); fetched
 * at most once even under concurrent callers. Failures return empty (callers
 * fall back to a host-derived label).
 */
internal interface ExternalLinkCatalogRepository {
    suspend fun sites(): List<ExternalLink>
}

/**
 * Enriches person biography links with the catalog's site name, icon and colour
 * (matched by host). Empty catalog (error/offline) leaves the links untouched
 * so the host-derived fallback still renders.
 */
internal suspend fun List<ExternalLink>.resolveWithCatalog(
    catalog: ExternalLinkCatalogRepository,
): List<ExternalLink> {
    if (isEmpty()) return this
    val sites = catalog.sites()
    if (sites.isEmpty()) return this
    return map { link ->
        matchCatalogSite(sites, link.url)?.let { site ->
            link.copy(
                site = site.site.ifBlank { link.site },
                iconUrl = site.iconUrl,
                colorHex = site.colorHex,
                type = site.type,
            )
        } ?: link
    }
}
