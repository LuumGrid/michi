package com.luum.michi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.materialkolor.PaletteStyle

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
 * `ui/` surfaces — renders inside this theme. The active [ThemeColors]
 * class is provided via [LocalThemeColors] and mapped to Material3.
 * Typography is [appTypography] over the [AppFont] catalog (bundled).
 */
@Composable
internal fun Theme(
    palette: ThemeColors = DefaultTheme(),
    type: ThemeType = ThemeType.SYSTEM,
    font: AppFont = AppFont.JAKARTA,
    surfaces: SurfaceStyle = SurfaceStyle.GLASS,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    content: @Composable () -> Unit,
) {
    val dark = type.resolveDarkTheme(isSystemInDarkTheme())
    val scheme = remember(palette, dark, style) { palette.scheme(dark, style) }
    val warning = remember(dark) { warningScheme(dark).primary }
    CompositionLocalProvider(
        LocalThemeColors provides palette,
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
