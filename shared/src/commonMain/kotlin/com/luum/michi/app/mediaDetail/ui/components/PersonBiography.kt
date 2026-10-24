package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.lazy.LazyListScope
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.common.BiographyStat
import com.luum.michi.app.mediaDetail.domain.common.ExternalLink

/**
 * The character/staff biography items: the collapsible prose (with the bold
 * stats) when there is text or stats, then the external link rows when there are
 * links. Same conditions and keys in both scopes.
 */
internal fun LazyListScope.personBiography(
    title: String,
    text: String,
    stats: List<BiographyStat>,
    spoilers: List<IntRange>,
    spoilerState: SpoilerRevealState,
    links: List<ExternalLink>,
    strings: LanguageStrings,
    onOpenUrl: (String) -> Unit,
) {
    if (text.isNotBlank() || stats.isNotEmpty()) {
        item(key = "description") {
            MediaDetailExpandableText(
                title = title,
                text = text,
                strings = strings,
                spoilerState = spoilerState,
                stats = stats,
                spoilers = spoilers,
            )
        }
    }
    if (links.isNotEmpty()) {
        item(key = "links") {
            MediaDetailLinkSection(
                links = links.map { it.toLinkItem(strings) },
                title = strings.mediaDetailExternalLinksTitle,
                onOpenUrl = onOpenUrl,
            )
        }
    }
}
