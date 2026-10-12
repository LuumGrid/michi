package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.GlassShape
import com.luum.michi.app.ui.components.glassBorder
import com.luum.michi.app.ui.components.glassContainerColor
import com.luum.michi.app.ui.components.solidBorder
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider

/**
 * Display-only pill row for the person heroes (favourites, occupations,
 * primary roles). Deliberately non-interactive: unlike the sort/filter
 * [com.luum.michi.app.ui.components.ChipRail] these pills carry no state.
 * Horizontally scrollable so long occupation lists never clip.
 */
@Composable
internal fun MediaDetailChipRow(
    chips: List<String>,
    modifier: Modifier = Modifier,
) {
    if (chips.isEmpty()) return
    val glass = SurfaceStyleProvider.current == SurfaceStyle.GLASS
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { label ->
            Surface(
                shape = GlassShape,
                color = if (glass) {
                    glassContainerColor()
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                border = if (glass) glassBorder() else solidBorder(),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
