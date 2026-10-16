package com.luum.michi.app.mediaDetail.domain.common

/**
 * The AniList catalog of external-link sites (name, url, icon, colour). Used to
 * resolve a person's biography links (which only carry a URL) into the same
 * shape media links already have. Cached in memory + disk (long TTL) and
 * fetched at most once, even under concurrent callers. Failures return empty
 * (callers fall back to a host-derived label) and are remembered briefly.
 */
internal interface ExternalLinkCatalogRepository {
    /**
     * Kicks a fetch in the catalog's own scope when the catalog is cold (no
     * data in memory and no request in flight). Never suspends and never
     * throws: the job outlives the caller, so a detail that fails or is
     * cancelled still warms the cache for the next one.
     */
    fun prefetch()

    /** The catalog, awaiting an in-flight [prefetch] or fetching on demand. */
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
