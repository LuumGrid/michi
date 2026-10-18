package com.luum.michi.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * Brand palette. Single source of truth: each entry carries its stable persisted
 * [id], its visible [displayName] and its [seed]. MaterialKolor generates the
 * full ~30-role M3 scheme (light + dark) from the seed via [scheme], so no role
 * falls back to generic Material defaults. The global, user-selected
 * [AppPaletteStyle] shapes how the scheme is generated from the seed.
 */
internal enum class AppPalette(
    val id: String,
    val displayName: String,
    val seed: Color,
) {
    DEFAULT("default", "Default", Color(0xFF5B5BD6)),
    OCEAN("ocean", "Ocean", Color(0xFF0288D1)),
    SAKURA("sakura", "Sakura", Color(0xFFEC407A)),
    FOREST("forest", "Forest", Color(0xFF2E7D32)),
    AMBER("amber", "Amber", Color(0xFFF59E0B)),
    VIOLET("violet", "Violet", Color(0xFF7C4DFF)),

    /** Low-chroma neutral seed; a full grayscale look comes from the
     *  [PaletteStyle.Monochrome] style. */
    MONO("mono", "Mono", Color(0xFF607D8B)),
    ;

    fun scheme(dark: Boolean, style: PaletteStyle = PaletteStyle.TonalSpot): ColorScheme =
        dynamicColorScheme(seedColor = seed, isDark = dark, isAmoled = false, style = style)
}

/**
 * The one palette lookup. Ids are explicit strings (never enum `name`) so a
 * rename never loses what the user has saved; unknown or null falls back to
 * [AppPalette.DEFAULT].
 */
internal fun paletteForId(id: String?): AppPalette =
    AppPalette.entries.firstOrNull { it.id == id } ?: AppPalette.DEFAULT
