package com.luum.michi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.SurfaceStyleProvider

/**
 * Shared "frosted glass" treatment for the floating [Toolbar] pills and the
 * [TabBar] capsule: translucent container + subtle border + soft shadow.
 * Faux glass on purpose — no backdrop-blur dependency; tune alphas on device.
 *
 * iOS-style grouping: a lone action is a [GlassCircle], adjacent same-side
 * actions merge into one capsule ([PillShape]).
 */
internal val GlassCircle: Shape = CircleShape

/** Shared translucency for every glass surface (bars and buttons alike). */
private const val GLASS_ALPHA = 0.88f

/** Visible diameter of the standard floating circle (Toolbar/TabBar actions). */
internal val GlassCircleSize = 48.dp

/** Visible diameter of a compact floating circle (a card/row overlay control). */
internal val GlassCircleCompactSize = 32.dp

/** Minimum touch target: a compact circle still gets a full 48dp circle to hit. */
internal val GlassTouchTargetSize = 48.dp

/** Icon inside [GlassCircleSize] / [GlassCircleCompactSize]. */
internal val GlassCircleIconSize = 24.dp
internal val GlassCircleCompactIconSize = 18.dp

/**
 * Compact elevation for small surfaces (48dp circles): still reads as
 * floating, but the smaller shadow fraction bleeds less through the
 * 0.88 fill instead of flooding the whole shape.
 */
internal val GlassElevationCompact = 16.dp

@Composable
internal fun glassContainerColor(): Color =
    MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = GLASS_ALPHA)

@Composable
internal fun glassBorder(): BorderStroke =
    BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )

/**
 * Recipe for a floating tool (Toolbar/TabBar, buttons, interactive chips,
 * trailing pills): glass under [SurfaceStyle.GLASS], flat otherwise. Tools never
 * branch themselves.
 */
@Composable
internal fun toolContainerColor(): Color =
    if (SurfaceStyleProvider.current == SurfaceStyle.GLASS) {
        glassContainerColor()
    } else {
        solidContainerColor()
    }

/**
 * Tool container for a tinted control (a brand-tinted button): a
 * [containerColor] is kept at [GLASS_ALPHA] under glass, opaque otherwise; null
 * falls back to the neutral tool container.
 */
@Composable
internal fun toolContainerColor(containerColor: Color?): Color =
    if (SurfaceStyleProvider.current == SurfaceStyle.GLASS) {
        containerColor?.copy(alpha = GLASS_ALPHA) ?: glassContainerColor()
    } else {
        containerColor ?: solidContainerColor()
    }

@Composable
internal fun toolBorder(): BorderStroke =
    if (SurfaceStyleProvider.current == SurfaceStyle.GLASS) {
        glassBorder()
    } else {
        flatBorder()
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
        color = toolContainerColor(),
        border = toolBorder(),
        // Elevation straight on the Surface (never a chained .shadow()):
        // the offscreen layer of Modifier.shadow seals translucent fills.
        shadowElevation = GlassElevationCompact,
        modifier = modifier,
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
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        shape = GlassCircle,
        color = toolContainerColor(),
        border = toolBorder(),
        // Elevation straight on the Surface (never a chained .shadow()):
        // the offscreen layer of Modifier.shadow seals translucent fills.
        shadowElevation = GlassElevationCompact,
        modifier = modifier,
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(GlassCircleSize)) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

/**
 * Compact variant of [GlassCircleButton]: a [GlassCircleCompactSize] visible
 * circle (card/row overlay control) that keeps a full [GlassTouchTargetSize]
 * circular touch area around it. Same tool recipe and elevation as the standard
 * circle, so it reads as the same floating piece, just smaller.
 */
@Composable
internal fun GlassCircleCompactButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(GlassTouchTargetSize)
            .clip(GlassCircle)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = GlassCircle,
            color = toolContainerColor(),
            border = toolBorder(),
            // Elevation straight on the Surface (never a chained .shadow()).
            shadowElevation = GlassElevationCompact,
        ) {
            Box(
                modifier = Modifier.size(GlassCircleCompactSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(GlassCircleCompactIconSize),
                )
            }
        }
    }
}

