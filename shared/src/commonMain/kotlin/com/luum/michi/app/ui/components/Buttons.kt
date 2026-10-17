package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** How much weight a button carries: the flow-closing action vs everything else. */
internal enum class ButtonEmphasis {
    /** The action that closes the flow (Save, Retry, Sign in): opaque fill. */
    Primary,

    /** Everything else: the floating tool recipe (glass or flat by `SurfaceStyle`). */
    Secondary,
}

/**
 * The one app button. [ButtonEmphasis.Secondary] is the floating tool recipe
 * (translucent glass under GLASS, flat `surface` otherwise); [Primary] is an
 * opaque `primaryContainer` fill with `onPrimaryContainer` text and no border,
 * so the flow-closing action reads as the main one. Same shape, height and
 * elevation in both. [containerColor]/[contentColor] override the recipe (brand
 * sign-in buttons, the red delete).
 */
@Composable
internal fun AppButton(
    label: String,
    onClick: () -> Unit,
    emphasis: ButtonEmphasis,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    shape: Shape = PillShape,
) {
    when (emphasis) {
        ButtonEmphasis.Secondary -> GlassButton(
            label = label,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = shape,
        )
        ButtonEmphasis.Primary -> {
            val container = containerColor ?: MaterialTheme.colorScheme.primaryContainer
            val content = contentColor ?: MaterialTheme.colorScheme.onPrimaryContainer
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = container,
                    contentColor = content,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp,
                ),
                contentPadding = PaddingValues(horizontal = 24.dp),
                modifier = modifier
                    .fillMaxWidth()
                    .height(GLASS_BUTTON_HEIGHT),
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = label)
            }
        }
    }
}
