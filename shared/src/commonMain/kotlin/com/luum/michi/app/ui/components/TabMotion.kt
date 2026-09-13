package com.luum.michi.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * Single source of truth for tab-switch motion (Telegram-style: smooth
 * cubic fade, zero springs). [TabBar] and `Root` share these so the bubble
 * and the feature content breathe as one animation.
 */
internal const val TabFadeMs = 250

internal fun <T> tabFadeSpec() = tween<T>(
    durationMillis = TabFadeMs,
    easing = FastOutSlowInEasing,
)
