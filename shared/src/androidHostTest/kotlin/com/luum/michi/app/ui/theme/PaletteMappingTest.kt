package com.luum.michi.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaletteMappingTest {

    // ── Palettes ──────────────────────────────────────────────────────────

    @Test
    fun paletteIdsRoundTrip() {
        AppPalette.entries.forEach { palette ->
            assertEquals(palette, paletteForId(palette.id))
        }
    }

    @Test
    fun paletteIdsAreUnique() {
        val ids = AppPalette.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun paletteIdsMatchTheStableList() {
        // Pinned on purpose: changing an id would silently drop every user's
        // saved palette, so an accidental edit must break this test.
        assertEquals(
            listOf("mono", "default", "ocean", "sakura", "forest", "amber", "violet"),
            AppPalette.entries.map { it.id },
        )
    }

    @Test
    fun unknownAndNullPaletteIdsFallBackToMono() {
        assertEquals(AppPalette.MONO, paletteForId(null))
        assertEquals(AppPalette.MONO, paletteForId(""))
        assertEquals(AppPalette.MONO, paletteForId("midnight"))
    }

    @Test
    fun paletteDisplayNames() {
        assertEquals(
            listOf("Mono", "Default", "Ocean", "Sakura", "Forest", "Amber", "Violet"),
            AppPalette.entries.map { it.displayName },
        )
    }

    @Test
    fun paletteSeedsMatchTheShippedHex() {
        // Pinned on purpose: a mis-copied hex would silently change the theme.
        assertEquals(Color(0xFF5B5BD6), AppPalette.DEFAULT.seed)
        assertEquals(Color(0xFF0288D1), AppPalette.OCEAN.seed)
        assertEquals(Color(0xFFEC407A), AppPalette.SAKURA.seed)
        assertEquals(Color(0xFF2E7D32), AppPalette.FOREST.seed)
        assertEquals(Color(0xFFF59E0B), AppPalette.AMBER.seed)
        assertEquals(Color(0xFF7C4DFF), AppPalette.VIOLET.seed)
        assertEquals(Color(0xFF607D8B), AppPalette.MONO.seed)
    }

    // ── Styles ────────────────────────────────────────────────────────────

    @Test
    fun styleIdsRoundTrip() {
        AppPaletteStyle.entries.forEach { style ->
            assertEquals(style, paletteStyleForId(style.id))
        }
    }

    @Test
    fun styleIdsAreUnique() {
        val ids = AppPaletteStyle.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun styleIdsMatchTheStableList() {
        // Pinned on purpose (same reason as the palette ids). Removed style ids
        // must keep falling back to Tonal spot.
        assertEquals(
            listOf("tonal_spot", "neutral", "vibrant", "rainbow", "fidelity"),
            AppPaletteStyle.entries.map { it.id },
        )
    }

    @Test
    fun unknownAndNullStyleIdsFallBackToTonalSpot() {
        assertEquals(AppPaletteStyle.TONAL_SPOT, paletteStyleForId(null))
        assertEquals(AppPaletteStyle.TONAL_SPOT, paletteStyleForId(""))
        assertEquals(AppPaletteStyle.TONAL_SPOT, paletteStyleForId("neon"))
        assertEquals(AppPaletteStyle.TONAL_SPOT, paletteStyleForId("expressive"))
        assertEquals(AppPaletteStyle.TONAL_SPOT, paletteStyleForId("fruit_salad"))
        assertEquals(AppPaletteStyle.TONAL_SPOT, paletteStyleForId("monochrome"))
        assertEquals(AppPaletteStyle.TONAL_SPOT, paletteStyleForId("content"))
    }

    @Test
    fun styleDisplayNames() {
        assertEquals(
            listOf("Tonal spot", "Neutral", "Vibrant", "Rainbow", "Fidelity"),
            AppPaletteStyle.entries.map { it.displayName },
        )
    }

    @Test
    fun stylesMapToTheFiveShippedMaterialStyles() {
        assertEquals(
            listOf(
                PaletteStyle.TonalSpot,
                PaletteStyle.Neutral,
                PaletteStyle.Vibrant,
                PaletteStyle.Rainbow,
                PaletteStyle.Fidelity,
            ),
            AppPaletteStyle.entries.map { it.materialStyle },
        )
    }

    @Test
    fun monoIgnoresTheSelectedStyle() {
        listOf(false, true).forEach { dark ->
            val monochrome = AppPalette.MONO.scheme(dark, PaletteStyle.Monochrome)
            AppPaletteStyle.entries.forEach { style ->
                assertEquals(
                    monochrome.roles(),
                    AppPalette.MONO.scheme(dark, style.materialStyle).roles(),
                    "MONO must ignore ${style.displayName} (dark=$dark)",
                )
            }
        }
    }

    @Test
    fun defaultFallsBackToTheFirstEntry() {
        assertTrue(paletteForId(null) == AppPalette.entries.first())
        assertTrue(paletteStyleForId(null) == AppPaletteStyle.entries.first())
    }

    /** ColorScheme has no equals(), so compare every role explicitly. */
    private fun ColorScheme.roles(): List<Color> = listOf(
        primary, onPrimary, primaryContainer, onPrimaryContainer, inversePrimary,
        secondary, onSecondary, secondaryContainer, onSecondaryContainer,
        tertiary, onTertiary, tertiaryContainer, onTertiaryContainer,
        background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant,
        surfaceTint, inverseSurface, inverseOnSurface,
        error, onError, errorContainer, onErrorContainer,
        outline, outlineVariant, scrim,
        surfaceBright, surfaceDim, surfaceContainer, surfaceContainerHigh,
        surfaceContainerHighest, surfaceContainerLow, surfaceContainerLowest,
        primaryFixed, primaryFixedDim, onPrimaryFixed, onPrimaryFixedVariant,
        secondaryFixed, secondaryFixedDim, onSecondaryFixed, onSecondaryFixedVariant,
        tertiaryFixed, tertiaryFixedDim, onTertiaryFixed, onTertiaryFixedVariant,
    )
}
