package com.luum.michi.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

/**
 * Motion sources for tab switching (250ms everywhere, zero springs).
 *
 * One organic language: symmetric ease-in-out (slow out, slow in) on
 * content, title and bubble alike — slow and fluid by choice.
 */
internal const val TabFadeMs = 300

internal fun <T> tabFadeSpec() = tween<T>(
    durationMillis = TabFadeMs,
    // Chill curve: gentle slope with nonzero end velocities, so something
    // visibly moves on every millisecond instead of bunching mid-way.
    easing = CubicBezierEasing(0.3f, 0.05f, 0.7f, 0.95f),
)

internal fun <T> tabBubbleSpec() = tween<T>(
    durationMillis = TabFadeMs,
    // Same chill curve as the content: one organic language everywhere.
    easing = CubicBezierEasing(0.3f, 0.05f, 0.7f, 0.95f),
)
