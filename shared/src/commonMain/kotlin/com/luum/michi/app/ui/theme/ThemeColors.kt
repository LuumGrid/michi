package com.luum.michi.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme

/**
 * Brand palette, mirroring the language tables (`LanguageStrings` + one
 * class per language). Each palette is a class holding only its seed color;
 * MaterialKolor generates the full ~30-role M3 scheme (light + dark) from it,
 * so no role falls back to generic Material defaults.
 */
internal interface ThemeColors {
    val seed: Color

    fun scheme(dark: Boolean): ColorScheme =
        dynamicColorScheme(seedColor = seed, isDark = dark, isAmoled = false)
}

/** Brand palette names travel raw on purpose (like "Michi"): not copy. */
internal val ThemePalettes: List<Pair<String, ThemeColors>> = listOf(
    "Default" to DefaultTheme(),
    "Ocean" to OceanTheme(),
    "Sakura" to SakuraTheme(),
)

/**
 * Stable persisted ids for the store. Explicit strings on purpose: class
 * names are fragile under obfuscation. Unknown ids fall back to Default.
 */
internal fun paletteForId(id: String?): ThemeColors = when (id) {
    "ocean" -> OceanTheme()
    "sakura" -> SakuraTheme()
    else -> DefaultTheme()
}

internal fun paletteIdOf(palette: ThemeColors): String = when (palette) {
    is OceanTheme -> "ocean"
    is SakuraTheme -> "sakura"
    else -> "default"
}

/** Display name for the current palette (brand literal, not copy). */
internal fun paletteDisplayName(palette: ThemeColors): String =
    ThemePalettes.firstOrNull { it.second::class == palette::class }?.first ?: "Default"
