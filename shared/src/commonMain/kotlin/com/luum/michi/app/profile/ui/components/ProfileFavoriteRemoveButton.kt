package com.luum.michi.app.profile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.toolBorder
import com.luum.michi.app.ui.components.toolContainerColor
import com.luum.michi.app.ui.icons.AppIcons

/**
 * The edit-mode "remove this favourite" control: a floating tool (glass/solid
 * circle) over a cover or trailing a settings-style row. Disabled while its
 * removal is in flight, so one item can never fire two requests.
 */
@Composable
internal fun ProfileFavoriteRemoveButton(
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = toolContainerColor(),
        border = toolBorder(),
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Clear,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
