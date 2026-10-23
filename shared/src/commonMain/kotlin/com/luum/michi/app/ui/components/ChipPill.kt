package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * The one interactive chip pill: selected = primary container, rest = the tool
 * recipe; ripple on tap. [compact] (cards) tightens padding and type so a card
 * chip lands where the list pills land; [iconUrl] draws a 16dp leading image
 * (platform logos) and [count] appends " · N". Shared by [ChipRail] and the
 * editor status rail. [leadingIcon] draws a 16dp leading icon tinted with
 * [leadingIconTint] (null = the chip's content color).
 */
@Composable
internal fun ChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
    iconUrl: String? = null,
    count: Int? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color? = null,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        shape = PillShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            toolContainerColor()
        },
        border = if (selected) null else toolBorder(),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = if (compact) 6.dp else 10.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!iconUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    loading = {},
                    error = {},
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = leadingIconTint ?: contentColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = if (count != null) "$label · $count" else label,
                style = if (compact) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.labelLarge
                },
                color = contentColor,
            )
        }
    }
}
