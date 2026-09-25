package com.luum.michi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared "frosted glass" treatment for the floating [Toolbar] pills and the
 * [TabBar] capsule: translucent container + subtle border + soft shadow.
 * Faux glass on purpose — no backdrop-blur dependency; tune alphas on device.
 *
 * iOS-style grouping: a lone action is a [GlassCircle], adjacent same-side
 * actions merge into one capsule ([GlassShape]).
 */
internal val GlassShape = RoundedCornerShape(24.dp)
internal val GlassCircle: Shape = CircleShape

/** Shared translucency for every glass surface (bars and buttons alike). */
private const val GLASS_ALPHA = 0.88f

@Composable
internal fun glassContainerColor(): Color =
    MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = GLASS_ALPHA)

@Composable
internal fun glassBorder(): BorderStroke =
    BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )

internal fun Modifier.glass(shape: Shape, elevation: Dp = 8.dp): Modifier =
    this
        .shadow(elevation = elevation, shape = shape)
        .clip(shape)

internal fun Modifier.glass(): Modifier = glass(GlassShape)

internal fun Modifier.glassPadding(top: Boolean): Modifier =
    if (top) {
        padding(start = 16.dp, top = 8.dp, end = 16.dp)
    } else {
        padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    }

/**
 * Floating glass capsule shared by the [TabBar] and sheet action bars:
 * translucent container + border + shadow, no positioning of its own
 * (callers own margins and interior padding).
 */
@Composable
internal fun GlassCapsule(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = GlassCircle,
        color = glassContainerColor(),
        border = glassBorder(),
        modifier = modifier.glass(GlassCircle),
    ) {
        content()
    }
}

/**
 * Fixed glass circle button (48dp touch target, no size overrides):
 * icon + action are injected, the recipe never changes. Use it for every
 * lone floating action so all circles measure identical.
 */
@Composable
internal fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int? = null,
) {
    Surface(
        shape = GlassCircle,
        color = glassContainerColor(),
        border = glassBorder(),
        modifier = modifier.glass(GlassCircle),
    ) {
        IconButton(onClick = onClick) {
            if (badgeCount != null && badgeCount > 0) {
                BadgedBox(badge = { Text(text = badgeCount.toString()) }) {
                    Icon(imageVector = icon, contentDescription = contentDescription)
                }
            } else {
                Icon(imageVector = icon, contentDescription = contentDescription)
            }
        }
    }
}

/**
 * Glass pill button: same recipe as the floating bars (translucent
 * container + border + soft shadow), so buttons and bars speak one style.
 * [containerColor] lets a brand tint the glass (kept at [GLASS_ALPHA]),
 * null means neutral glass. The shadow comes from [glass], not M3 elevation.
 * Fixed [GLASS_BUTTON_HEIGHT] so pills match the 48dp toolbar tools.
 */
internal val GLASS_BUTTON_HEIGHT = 48.dp

@Composable
internal fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    shape: Shape = GlassShape,
) {
    val container = containerColor?.copy(alpha = GLASS_ALPHA) ?: glassContainerColor()
    val content = contentColor ?: MaterialTheme.colorScheme.onSurface
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        border = glassBorder(),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(GLASS_BUTTON_HEIGHT)
            .glass(shape),
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
