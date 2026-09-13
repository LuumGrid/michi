package com.luum.michi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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

@Composable
internal fun glassContainerColor(): Color =
    MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = 0.88f)

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
