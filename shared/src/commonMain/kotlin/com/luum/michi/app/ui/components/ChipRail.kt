package com.luum.michi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider

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
            val isSelected = interactive && tab.value == selected
            val glass = interactive && SurfaceStyleProvider.current == SurfaceStyle.GLASS
            val color = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                glass -> glassContainerColor()
                else -> MaterialTheme.colorScheme.surface
            }
            val border = when {
                isSelected -> null
                glass -> glassBorder()
                else -> BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (interactive) {
                Surface(
                    onClick = { onSelect(tab.value) },
                    shape = GlassShape,
                    color = color,
                    border = border,
                ) {
                    ChipPillContent(tab = tab, compact = compact, isSelected = isSelected)
                }
            } else {
                Surface(
                    shape = GlassShape,
                    color = color,
                    border = border,
                ) {
                    ChipPillContent(tab = tab, compact = compact, isSelected = isSelected)
                }
            }
        }
    }
}

@Composable
private fun <T> ChipPillContent(tab: ChipTab<T>, compact: Boolean, isSelected: Boolean) {
    Row(
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = if (compact) 6.dp else 10.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!tab.iconUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = tab.iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                loading = {},
                error = {},
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = if (tab.count != null) "${tab.label} · ${tab.count}" else tab.label,
            style = if (compact) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.labelLarge
            },
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
