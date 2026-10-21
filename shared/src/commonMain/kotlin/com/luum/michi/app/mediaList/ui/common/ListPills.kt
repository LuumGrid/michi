package com.luum.michi.app.mediaList.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.PillEndShape
import com.luum.michi.app.ui.components.PillShape
import com.luum.michi.app.ui.components.flatBorder

/**
 * Counter + increment as one fused capsule: they share a single border
 * with no air between them (a spacer would draw a double seam). The +1
 * half carries the primary container; the counter half stays ghost.
 * Card taps open the editor (AL-chan pattern: implicit edit, no
 * button); cover/title taps will navigate to detail.
 */
@Composable
internal fun ProgressGroup(
    progress: String,
    action: String,
    showAction: Boolean,
    enabled: Boolean,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surface,
        border = flatBorder(),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = progress,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            // +1 only where progress can move: elsewhere the counter stands
            // alone — showing a dead action would imply it works.
            if (showAction) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(PillEndShape)
                        .background(
                            if (enabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        )
                        .clickable(
                            enabled = enabled,
                            onClick = onIncrement,
                            role = Role.Button,
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = action,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}
