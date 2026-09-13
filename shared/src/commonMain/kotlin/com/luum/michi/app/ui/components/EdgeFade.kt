package com.luum.michi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

/**
 * Anchor veils: surface-color gradients painted BEHIND the floating bars so
 * the glass settles over scrolling content instead of hovering raw.
 * Alphas tuned on device; stops are approximations of the spec.
 */
private const val VEIL_PEAK = 0.5f

@Composable
internal fun TopEdgeFade(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0.0f to base.copy(alpha = VEIL_PEAK),
                0.35f to base.copy(alpha = VEIL_PEAK),
                0.75f to base.copy(alpha = 0.15f),
                1.0f to base.copy(alpha = 0f),
            ),
        ),
    )
}

@Composable
internal fun BottomEdgeFade(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0.0f to base.copy(alpha = 0f),
                0.45f to base.copy(alpha = 0f),
                0.7f to base.copy(alpha = VEIL_PEAK),
                1.0f to base.copy(alpha = VEIL_PEAK),
            ),
        ),
    )
}
