package com.luum.michi.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Brand buttons on the shared glass recipe (see [GlassButton]), so the auth
 * surface matches the floating bars instead of mixing M3-solid + glass.
 * Brand colors arrive as raw [Color]s (converted at the boundary); null
 * means "use the theme".
 */
@Composable
internal fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    GlassButton(
        label = label,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

@Composable
internal fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlassButton(
        label = label,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = null,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
