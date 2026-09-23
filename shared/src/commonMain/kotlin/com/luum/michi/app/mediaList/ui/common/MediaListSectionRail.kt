package com.luum.michi.app.mediaList.ui.common

import androidx.compose.foundation.BorderStroke
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
import com.luum.michi.app.ui.components.GlassShape

/**
 * Section rail shared by the anime/manga lists. Generic over the scope's
 * section type so `anime/` and `manga/` never depend on each other: each
 * caller passes its own sections in and gets its own type back on select.
 * Labels arrive translated with holder counts; this composable owns only
 * the pill language (selected = primary container, rest = outlined).
 */
data class MediaListSectionTab<T>(
    val value: T,
    val label: String,
    val count: Int,
)

@Composable
internal fun <T> MediaListSectionRail(
    tabs: List<MediaListSectionTab<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = tabs,
            key = { it.label },
        ) { tab ->
            val isSelected = tab.value == selected
            Surface(
                onClick = { onSelect(tab.value) },
                shape = GlassShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = if (isSelected) {
                    null
                } else {
                    BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                },
            ) {
                Text(
                    text = "${tab.label} · ${tab.count}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}
