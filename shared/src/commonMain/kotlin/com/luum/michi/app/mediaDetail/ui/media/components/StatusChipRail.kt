package com.luum.michi.app.mediaDetail.ui.media.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.ui.components.GlassShape
import com.luum.michi.app.ui.components.glassBorder
import com.luum.michi.app.ui.components.glassContainerColor
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider

/**
 * Single-select status chips in a horizontal rail (the editor goes rail,
 * not rows, for the 6 statuses). Plain Row + manual scroll on purpose:
 * a LazyRow nested in the sheet's scroll column mismeasures (overlapping
 * rows); this one sizes itself like any other row. Auto-scrolls to the
 * selection; no counts (unlike the list section rails) — selection is
 * the only state.
 */
/**
 * Single-select status chips in a horizontal rail (the editor goes rail,
 * not rows, for the 6 statuses). Plain Row + manual scroll on purpose:
 * a LazyRow nested in the sheet's scroll column mismeasures (overlapping
 * rows); this one sizes itself like any other row. No auto-scroll (the
 * first chips are always visible; the rest are tapped in view) and no
 * counts (unlike the list section rails) — selection is the only state.
 */
@Composable
internal fun StatusChipRail(
    options: List<MediaListStatus>,
    labels: List<String>,
    selected: MediaListStatus,
    onSelect: (MediaListStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            val glass = SurfaceStyleProvider.current == SurfaceStyle.GLASS
            Surface(
                onClick = { onSelect(option) },
                shape = GlassShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else if (glass) {
                    glassContainerColor()
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = if (isSelected) {
                    null
                } else if (glass) {
                    glassBorder()
                } else {
                    BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                },
            ) {
                Text(
                    text = labels[index],
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
