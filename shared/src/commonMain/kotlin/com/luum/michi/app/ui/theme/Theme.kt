package com.luum.michi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/** Theme selection types. SYSTEM follows the OS; LIGHT/DARK force a scheme. */
internal enum class ThemeType {
    SYSTEM,
    LIGHT,
    DARK,
}

internal fun ThemeType.resolveDarkTheme(systemDark: Boolean): Boolean = when (this) {
    ThemeType.SYSTEM -> systemDark
    ThemeType.LIGHT -> false
    ThemeType.DARK -> true
}

/**
 * Global app theme. Every screen — current placeholders and future feature
 * `ui/` surfaces — renders inside this theme. The active [AppPalette]
 * entry is mapped to Material3.
 * Typography is [appTypography] over the [AppFont] catalog (bundled).
 */
@Composable
internal fun Theme(
    palette: AppPalette = AppPalette.MONO,
    type: ThemeType = ThemeType.SYSTEM,
    font: AppFont = AppFont.JAKARTA,
    surfaces: SurfaceStyle = SurfaceStyle.GLASS,
    style: AppPaletteStyle = AppPaletteStyle.TONAL_SPOT,
    content: @Composable () -> Unit,
) {
    val dark = type.resolveDarkTheme(isSystemInDarkTheme())
    val scheme = remember(palette, dark, style) { palette.scheme(dark, style.materialStyle) }
    val warning = remember(dark) { warningScheme(dark).primary }
    CompositionLocalProvider(
        LocalWarning provides warning,
        LocalSurfaceStyle provides surfaces,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = appTypography(font),
            content = content,
        )
    }
}
