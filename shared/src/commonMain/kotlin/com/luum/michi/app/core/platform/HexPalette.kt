package com.luum.michi.app.core.platform

import androidx.compose.ui.graphics.Color

private val FallbackPalette = listOf(
    Color(0xFF312E81),
    Color(0xFFEC4899),
)

/**
 * Converts an AniList `coverImage.color` hex string (e.g. `"#f1785d"`) into a
 * 2-stop palette suitable for gradient backgrounds. Returns a neutral fallback
 * when the input is null, blank, or unparseable.
 */
fun hexToPalette(hex: String?): List<Color> {
    if (hex.isNullOrBlank()) return FallbackPalette
    val cleaned = hex.removePrefix("#")
    if (cleaned.length !in setOf(6, 8)) return FallbackPalette
    val parsed = cleaned.toLongOrNull(16) ?: return FallbackPalette
    val rgb = if (cleaned.length == 6) parsed or 0xFF000000L else parsed
    val base = Color(rgb)
    val darker = Color(
        red = (base.red * 0.55f).coerceIn(0f, 1f),
        green = (base.green * 0.55f).coerceIn(0f, 1f),
        blue = (base.blue * 0.55f).coerceIn(0f, 1f),
        alpha = 1f,
    )
    return listOf(darker, base)
}
