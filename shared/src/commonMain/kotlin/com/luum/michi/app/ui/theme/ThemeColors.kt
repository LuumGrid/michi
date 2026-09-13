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
