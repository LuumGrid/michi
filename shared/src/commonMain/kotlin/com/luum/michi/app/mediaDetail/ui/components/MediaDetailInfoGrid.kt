package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One label/value cell of a [MediaDetailInfoGrid]. */
internal data class MediaDetailInfoEntry(val label: String, val value: String)

/**
 * The one information grid of the detail panels (media, character, staff,
 * studio): label above value. [entries] pair up two per row (an odd tail keeps
 * half width instead of stretching) and [wideEntries] take the full width for
 * long values (titles, alternate names).
 */
@Composable
internal fun MediaDetailInfoGrid(
    entries: List<MediaDetailInfoEntry>,
    modifier: Modifier = Modifier,
    wideEntries: List<MediaDetailInfoEntry> = emptyList(),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        entries.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                pair.forEach { entry ->
                    InfoCell(
                        label = entry.label,
                        value = entry.value,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Odd tail: keep the lone cell half width, not stretched.
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
        wideEntries.forEach { entry ->
            InfoCell(
                label = entry.label,
                value = entry.value,
                valueMaxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Label above value: reads cleanly in a two-column grid and full width alike. */
@Composable
private fun InfoCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueMaxLines: Int = 2,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = valueMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
