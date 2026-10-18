package com.luum.michi.app.ui.theme

import com.materialkolor.PaletteStyle

/**
 * Global palette style applied to whichever [AppPalette] seed is selected.
 * Single source of truth: each entry carries its stable persisted [id], its
 * visible [displayName] and the MaterialKolor [materialStyle] it wraps. Calm
 * first (the default), then the more chromatic/playful ones. Names are Material
 * terms and travel raw (like palette names): not copy.
 */
internal enum class AppPaletteStyle(
    val id: String,
    val displayName: String,
    val materialStyle: PaletteStyle,
) {
    TONAL_SPOT("tonal_spot", "Tonal spot", PaletteStyle.TonalSpot),
    NEUTRAL("neutral", "Neutral", PaletteStyle.Neutral),
    VIBRANT("vibrant", "Vibrant", PaletteStyle.Vibrant),
    EXPRESSIVE("expressive", "Expressive", PaletteStyle.Expressive),
    RAINBOW("rainbow", "Rainbow", PaletteStyle.Rainbow),
    FRUIT_SALAD("fruit_salad", "Fruit salad", PaletteStyle.FruitSalad),
    MONOCHROME("monochrome", "Monochrome", PaletteStyle.Monochrome),
    FIDELITY("fidelity", "Fidelity", PaletteStyle.Fidelity),
    CONTENT("content", "Content", PaletteStyle.Content),
}

/**
 * The one style lookup. Ids are explicit strings on purpose (stable under a
 * rename); unknown or null falls back to [AppPaletteStyle.TONAL_SPOT].
 */
internal fun paletteStyleForId(id: String?): AppPaletteStyle =
    AppPaletteStyle.entries.firstOrNull { it.id == id } ?: AppPaletteStyle.TONAL_SPOT
