package com.luum.michi.app.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout

@Composable
internal fun shellCollapsibleChipsModifier(
    collapseFraction: Float,
    base: Modifier = Modifier,
): Modifier = base
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val naturalH = placeable.height
        val visibleH = (naturalH * (1f - collapseFraction)).toInt().coerceAtLeast(0)
        layout(placeable.width, visibleH) {
            placeable.place(0, -(naturalH * collapseFraction).toInt())
        }
    }
    .graphicsLayer { alpha = 1f - collapseFraction }
