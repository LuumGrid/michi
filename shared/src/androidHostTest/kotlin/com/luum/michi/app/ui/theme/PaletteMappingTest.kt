package com.luum.michi.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaletteMappingTest {

    @Test
    fun paletteIdsRoundTrip() {
        val ids = setOf("default", "ocean", "sakura")
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
        assertEquals(3, ThemePalettes.size)
        assertEquals(
            setOf("default", "ocean", "sakura"),
            ThemePalettes.map { paletteIdOf(it.second) }.toSet(),
        )
    }
}
