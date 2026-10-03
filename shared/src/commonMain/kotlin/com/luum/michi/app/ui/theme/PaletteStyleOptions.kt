package com.luum.michi.app.ui.theme

import com.materialkolor.PaletteStyle

/**
 * Global palette style applied to whichever [ThemeColors] seed is selected.
 * Calm first (the default), then the more chromatic/playful ones. Names are
 * Material terms and travel raw (like palette names): not copy.
 */
internal val PaletteStyles: List<PaletteStyle> = listOf(
    PaletteStyle.TonalSpot,
    PaletteStyle.Neutral,
    PaletteStyle.Vibrant,
    PaletteStyle.Expressive,
    PaletteStyle.Rainbow,
    PaletteStyle.FruitSalad,
    PaletteStyle.Monochrome,
    PaletteStyle.Fidelity,
    PaletteStyle.Content,
)

/**
 * Stable persisted ids for the store. Explicit strings on purpose: enum
 * `name` is fine for enums compiled in, but these stay stable if the enum
 * is ever renamed. Unknown ids fall back to TonalSpot.
 */
internal fun paletteStyleForId(id: String?): PaletteStyle = when (id) {
    "neutral" -> PaletteStyle.Neutral
    "vibrant" -> PaletteStyle.Vibrant
    "expressive" -> PaletteStyle.Expressive
    "rainbow" -> PaletteStyle.Rainbow
    "fruit_salad" -> PaletteStyle.FruitSalad
    "monochrome" -> PaletteStyle.Monochrome
    "fidelity" -> PaletteStyle.Fidelity
    "content" -> PaletteStyle.Content
    else -> PaletteStyle.TonalSpot
}

internal fun paletteStyleIdOf(style: PaletteStyle): String = when (style) {
    PaletteStyle.Neutral -> "neutral"
    PaletteStyle.Vibrant -> "vibrant"
    PaletteStyle.Expressive -> "expressive"
    PaletteStyle.Rainbow -> "rainbow"
    PaletteStyle.FruitSalad -> "fruit_salad"
    PaletteStyle.Monochrome -> "monochrome"
    PaletteStyle.Fidelity -> "fidelity"
    PaletteStyle.Content -> "content"
    PaletteStyle.TonalSpot -> "tonal_spot"
}

internal fun paletteStyleDisplayName(style: PaletteStyle): String = when (style) {
    PaletteStyle.TonalSpot -> "Tonal spot"
    PaletteStyle.Neutral -> "Neutral"
    PaletteStyle.Vibrant -> "Vibrant"
    PaletteStyle.Expressive -> "Expressive"
    PaletteStyle.Rainbow -> "Rainbow"
    PaletteStyle.FruitSalad -> "Fruit salad"
    PaletteStyle.Monochrome -> "Monochrome"
    PaletteStyle.Fidelity -> "Fidelity"
    PaletteStyle.Content -> "Content"
}
