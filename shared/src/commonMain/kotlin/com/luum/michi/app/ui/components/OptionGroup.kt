package com.luum.michi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider

/**
 * IG-style framed group: optional subtitle + rounded frame enclosing
 * option rows. Rows draw their own dividers ([OptionRow.divider]).
 *
 * Both surface styles keep the same frame structure, padding and sizes —
 * only the recipe changes (translucent + glass border + shadow vs opaque
 * + outline border). The experiment isolates surface, never layout.
 */
internal val OptionGroupShape = RoundedCornerShape(20.dp)

@Composable
internal fun OptionGroup(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = SurfaceStyleProvider.current == SurfaceStyle.GLASS
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Surface(
            shape = OptionGroupShape,
            border = if (glass) {
                glassBorder()
            } else {
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            },
            // Translucent fill in glass mode; opaque surface in solid.
            // Same frame, padding and sizes in both — only the recipe
            // changes, so the experiment isolates surface, never layout.
            color = if (glass) {
                glassContainerColor()
            } else {
                MaterialTheme.colorScheme.surface
            },
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                content()
            }
        }
    }
}
