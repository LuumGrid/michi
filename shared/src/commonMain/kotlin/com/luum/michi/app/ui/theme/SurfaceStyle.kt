package com.luum.michi.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * App-wide surface style: frosted glass or solid opaque. Read everywhere
 * a container picks its recipe (sheet interiors and floating chrome
 * alike) via [SurfaceStyleProvider]. Both styles are first-class and
 * permanent: every surface implements both recipes with identical
 * metrics, so only the recipe ever varies.
 */
internal enum class SurfaceStyle {
    GLASS,
    SOLID,
}

internal val LocalSurfaceStyle = compositionLocalOf { SurfaceStyle.GLASS }

internal object SurfaceStyleProvider {
    val current: SurfaceStyle
        @Composable get() = LocalSurfaceStyle.current
}
