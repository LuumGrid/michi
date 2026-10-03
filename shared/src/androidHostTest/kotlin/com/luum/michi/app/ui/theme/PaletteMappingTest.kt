package com.luum.michi.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaletteMappingTest {

    @Test
    fun paletteIdsRoundTrip() {
        val ids = setOf("default", "ocean", "sakura", "forest", "amber", "violet", "mono")
        assertEquals(ids, ids.map { paletteIdOf(paletteForId(it)) }.toSet())
    }

    @Test
    fun unknownAndNullIdsFallBackToDefault() {
        assertTrue(paletteForId(null) is DefaultTheme)
        assertTrue(paletteForId("") is DefaultTheme)
        assertTrue(paletteForId("midnight") is DefaultTheme)
    }

    @Test
    fun paletteDisplayNames() {
        assertEquals("Default", paletteDisplayName(DefaultTheme()))
        assertEquals("Ocean", paletteDisplayName(OceanTheme()))
        assertEquals("Sakura", paletteDisplayName(SakuraTheme()))
        assertEquals("Forest", paletteDisplayName(ForestTheme()))
        assertEquals("Amber", paletteDisplayName(AmberTheme()))
        assertEquals("Violet", paletteDisplayName(VioletTheme()))
        assertEquals("Mono", paletteDisplayName(MonoTheme()))
    }

    @Test
    fun unknownPaletteFallsBackToDefaultName() {
        val custom = object : ThemeColors {
            override val seed: Color = Color.Red
        }
        assertEquals("Default", paletteDisplayName(custom))
    }

    @Test
    fun palettesListCoversEveryId() {
        assertEquals(7, ThemePalettes.size)
        assertEquals(
            setOf("default", "ocean", "sakura", "forest", "amber", "violet", "mono"),
            ThemePalettes.map { paletteIdOf(it.second) }.toSet(),
        )
    }

    @Test
    fun styleIdsRoundTrip() {
        assertEquals(PaletteStyles.toSet(), PaletteStyles.map { paletteStyleForId(paletteStyleIdOf(it)) }.toSet())
    }

    @Test
    fun unknownStyleFallsBackToTonalSpot() {
        assertEquals(PaletteStyle.TonalSpot, paletteStyleForId(null))
        assertEquals(PaletteStyle.TonalSpot, paletteStyleForId(""))
        assertEquals(PaletteStyle.TonalSpot, paletteStyleForId("neon"))
    }

    @Test
    fun styleDisplayNames() {
        assertEquals("Tonal spot", paletteStyleDisplayName(PaletteStyle.TonalSpot))
        assertEquals("Monochrome", paletteStyleDisplayName(PaletteStyle.Monochrome))
        assertEquals("Fruit salad", paletteStyleDisplayName(PaletteStyle.FruitSalad))
    }

    @Test
    fun stylesListCoversEveryMaterialStyle() {
        assertEquals(PaletteStyle.entries.toSet(), PaletteStyles.toSet())
    }
}
