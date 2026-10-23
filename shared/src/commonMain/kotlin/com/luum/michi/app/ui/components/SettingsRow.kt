package com.luum.michi.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.icons.AppIcons

/**
 * Settings-style row: label + current/secondary value, with a trailing chevron
 * when tappable (static when [onClick] is null). Rows draw their own dividers, so
 * set [divider]=false on the first row of a group. Shared by Settings and by
 * Explore's studio results (settings look: name + "Anime studio"/"Producer").
 * Optional leading [icon] (20dp, onSurfaceVariant) and a [supportingText] line
 * under the label (About page rows).
 */
@Composable
internal fun SettingsRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    divider: Boolean = true,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    // Trailing widget instead of the chevron (e.g. an edit-mode remove button).
    // It is painted as a centered end overlay, so a 48dp control never grows
    // the row (the same row measures the same height in both modes).
    trailing: (@Composable () -> Unit)? = null,
    icon: ImageVector? = null,
    supportingText: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (divider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (supportingText != null) {
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = valueColor,
                )
                if (trailing != null) {
                    // The overlay's footprint: 48dp touch target + the usual 8dp gap.
                    Spacer(modifier = Modifier.width(TrailingSlotWidth))
                } else if (onClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (trailing != null) {
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    trailing()
                }
            }
        }
    }
}

/** 48dp touch target + 8dp gap, reserved inside the text row. */
private val TrailingSlotWidth = 56.dp
