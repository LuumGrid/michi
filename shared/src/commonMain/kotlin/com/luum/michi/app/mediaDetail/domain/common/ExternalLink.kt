package com.luum.michi.app.mediaDetail.domain.common

import com.luum.michi.app.core.language.domain.LanguageStrings

/**
 * A normalized external link, used for both media links (from the API) and
 * person biography links (resolved against the site catalog). `site` is the
 * display name, `handle` an optional `@user` shown in the secondary slot.
 */
internal data class ExternalLink(
    val site: String,
    val url: String,
    val iconUrl: String? = null,
    val colorHex: String? = null,
    val type: String? = null,
    val language: String? = null,
    val handle: String? = null,
)

/** The API marks the official site by name (its `type` is a generic INFO). */
internal fun ExternalLink.isOfficialSite(): Boolean = site.equals("Official Site", ignoreCase = true)

/** Localized label for a known external-link site name (API sends English). */
internal fun ExternalLink.siteLabel(strings: LanguageStrings): String =
    if (isOfficialSite()) strings.mediaDetailOfficialSiteLabel else site
