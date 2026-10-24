package com.luum.michi.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Converts a raw brand hex (`"#02A9FF"` / `"02A9FF"`) into a [Color].
 * Per project rules the palette travels across layers as a raw String and is
 * converted here, at the UI boundary. Falls back to transparent on bad input.
 */
internal fun brandColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val withAlpha = when (cleaned.length) {
        6 -> "FF$cleaned"
        8 -> cleaned
        else -> return Color.Transparent
    }
    val value = withAlpha.toLongOrNull(radix = 16) ?: return Color.Transparent
    return Color(value)
}
