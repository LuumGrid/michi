package com.luum.michi.app.ui.theme

import androidx.compose.ui.graphics.Color
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
            listOf("default", "ocean", "sakura", "forest", "amber", "violet", "mono"),
            AppPalette.entries.map { it.id },
        )
    }

    @Test
    fun unknownAndNullPaletteIdsFallBackToDefault() {
        assertEquals(AppPalette.DEFAULT, paletteForId(null))
        assertEquals(AppPalette.DEFAULT, paletteForId(""))
        assertEquals(AppPalette.DEFAULT, paletteForId("midnight"))
    }

    @Test
    fun paletteDisplayNames() {
        assertEquals(
            listOf("Default", "Ocean", "Sakura", "Forest", "Amber", "Violet", "Mono"),
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
        // Pinned on purpose (same reason as the palette ids).
        assertEquals(
            listOf(
                "tonal_spot",
                "neutral",
                "vibrant",
                "expressive",
                "rainbow",
                "fruit_salad",
                "monochrome",
                "fidelity",
                "content",
            ),
            AppPaletteStyle.entries.map { it.id },
        )
    }

    @Test
    fun unknownAndNullStyleIdsFallBackToTonalSpot() {
        assertEquals(AppPaletteStyle.TONAL_SPOT, paletteStyleForId(null))
        assertEquals(AppPaletteStyle.TONAL_SPOT, paletteStyleForId(""))
        assertEquals(AppPaletteStyle.TONAL_SPOT, paletteStyleForId("neon"))
    }

    @Test
    fun styleDisplayNames() {
        assertEquals(
            listOf(
                "Tonal spot",
                "Neutral",
                "Vibrant",
                "Expressive",
                "Rainbow",
                "Fruit salad",
                "Monochrome",
                "Fidelity",
                "Content",
            ),
            AppPaletteStyle.entries.map { it.displayName },
        )
    }

    @Test
    fun stylesCoverEveryMaterialStyle() {
        assertEquals(
            com.materialkolor.PaletteStyle.entries.toSet(),
            AppPaletteStyle.entries.map { it.materialStyle }.toSet(),
        )
    }

    @Test
    fun defaultFallsBackToTheFirstEntry() {
        assertTrue(paletteForId(null) == AppPalette.entries.first())
        assertTrue(paletteStyleForId(null) == AppPaletteStyle.entries.first())
    }
}
