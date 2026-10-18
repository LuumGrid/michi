package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Generic chip rail: section pills for the lists, year pills in filter
 * sheets, platform chips on calendar cards. Generic over the tab type so
 * features never depend on each other: each caller passes its own tabs in
 * and gets its own type back on select. Labels arrive translated; a null
 * [ChipTab.count] renders the label alone (display-only chips), a null
 * [selected] leaves every pill unhighlighted. This composable owns only
 * the pill language (selected = primary container, rest = outlined).
 *
 * [interactive] false renders **flat, inert** chips (`surface` + `outlineVariant`
 * border, no glass, no click/ripple): they are labels (genres, tags, roles,
 * occupations, studios), not buttons. The interactive recipe is unchanged.
 */
data class ChipTab<T>(
    val value: T,
    val label: String,
    val count: Int? = null,
    // Platform logo (calendar chips): 16dp image ahead of the label,
    // hidden when null, blank or failed (text alone still reads).
    val iconUrl: String? = null,
)

@Composable
internal fun <T> ChipRail(
    tabs: List<ChipTab<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    // Baked 16dp matches the full-width list screens; sheets (already
    // inset) pass zero to avoid double padding.
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    // Compact chips (cards): tighter padding + smaller type so the bottom
    // edge lands where the list pills land. Defaults keep every other
    // caller byte-identical.
    compact: Boolean = false,
    // Display-only chips: flat surface + outline, no ripple.
    interactive: Boolean = true,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = tabs,
            key = { "${it.label}·${it.value}" },
        ) { tab ->
            if (interactive) {
                ChipPill(
                    label = tab.label,
                    selected = tab.value == selected,
                    onClick = { onSelect(tab.value) },
                    compact = compact,
                    iconUrl = tab.iconUrl,
                    count = tab.count,
                )
            } else {
                FlatChip(
                    label = if (tab.count != null) "${tab.label} · ${tab.count}" else tab.label,
                )
            }
        }
    }
}

/**
 * Flat, inert chip — the one flat-chip recipe (`surface` + `outlineVariant`, no
 * glass, no click). `ChipRail`'s `interactive = false` branch and the person
 * hero's chip `FlowRow` both draw it, so the style never forks. A flat chip
 * carries only a label: `iconUrl`/`compact` belong to the interactive rail.
 */
@Composable
internal fun FlatChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surface,
        border = flatBorder(),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
