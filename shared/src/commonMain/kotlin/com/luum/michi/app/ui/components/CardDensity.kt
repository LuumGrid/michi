package com.luum.michi.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Caps the OS font scale inside dense cards (list rows, rail cards), so large
 * text settings degrade to ellipsis instead of clipping the content or pushing
 * cover badges into each other (1.3× still keeps the badge row apart on the
 * smallest cover). Full-scale access lives on the detail screens (unbounded
 * layouts).
 */
@Composable
internal fun CardCappedDensity(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val capped = remember(density) {
        Density(density.density, density.fontScale.coerceAtMost(CardFontScaleCap))
    }
    CompositionLocalProvider(LocalDensity provides capped, content = content)
}

internal const val CardFontScaleCap = 1.3f
