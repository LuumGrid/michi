package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.SectionHeader

/**
 * Framed information panel shared by the media/character/staff scopes: the
 * section title, a 12dp gap, the two-column [MediaDetailInfoGrid] and an optional
 * [footer] inside the same column (the media panel's studio chip groups).
 */
@Composable
internal fun InfoPanel(
    entries: List<MediaDetailInfoEntry>,
    wideEntries: List<MediaDetailInfoEntry>,
    strings: LanguageStrings,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    MediaDetailPanel {
        SectionHeader(strings.mediaDetailInformationTitle)
        Spacer(modifier = Modifier.height(12.dp))
        MediaDetailInfoGrid(entries = entries, wideEntries = wideEntries)
        footer?.invoke(this)
    }
}

/** Adds a label/value cell unless the value is null or blank. */
internal fun MutableList<MediaDetailInfoEntry>.addInfo(label: String, value: String?) {
    if (!value.isNullOrBlank()) add(MediaDetailInfoEntry(label, value))
}

/** The alternate-names cell: blanks dropped and joined with ", ", or null when none. */
internal fun alternativeNamesEntry(names: List<String>, label: String): MediaDetailInfoEntry? {
    val joined = names.filter { it.isNotBlank() }.joinToString(", ")
    return joined.takeIf { it.isNotBlank() }?.let { MediaDetailInfoEntry(label, it) }
}
