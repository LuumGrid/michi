package com.luum.michi.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Exposes the active [ThemeColors], mirroring the language provider.
 * Prefer `MaterialTheme.colorScheme` for standard roles; use this for
 * direct access to the brand slots.
 */
internal val LocalThemeColors = compositionLocalOf<ThemeColors> { DefaultTheme() }

internal object ThemeProvider {
    val colors: ThemeColors
        @Composable get() = LocalThemeColors.current
}
