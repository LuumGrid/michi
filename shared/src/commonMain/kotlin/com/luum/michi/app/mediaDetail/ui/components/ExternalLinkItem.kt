package com.luum.michi.app.mediaDetail.ui.components

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.common.ExternalLink
import com.luum.michi.app.mediaDetail.domain.common.isOfficialSite
import com.luum.michi.app.mediaDetail.domain.common.siteLabel

/**
 * The one mapping from a domain [ExternalLink] to the shared link row: used by
 * media (already resolved by the API) and by staff/character biography links
 * (resolved against the site catalog). `detail` shows the language for media
 * links and the `@handle` for people.
 */
internal fun ExternalLink.toLinkItem(strings: LanguageStrings): MediaDetailLinkItem = MediaDetailLinkItem(
    label = siteLabel(strings),
    url = url,
    iconUrl = iconUrl,
    colorHex = colorHex,
    isOfficialSite = isOfficialSite(),
    detail = language ?: handle,
)
