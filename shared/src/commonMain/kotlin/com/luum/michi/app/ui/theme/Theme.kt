package com.luum.michi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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
    content: @Composable () -> Unit,
) {
    val dark = type.resolveDarkTheme(isSystemInDarkTheme())
    CompositionLocalProvider(
        LocalThemeColors provides palette,
        LocalWarning provides warningScheme(dark).primary,
        LocalSurfaceStyle provides surfaces,
    ) {
        MaterialTheme(
            colorScheme = palette.scheme(dark),
            typography = appTypography(font),
            content = content,
        )
    }
}
