package com.luum.michi.app.ui

import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.CoverSize
import com.luum.michi.app.ui.components.gridCoverSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A poster grid sizes its cells from a standard token: [CoverSize.Featured]
 * while at least two of them fit with air, otherwise the next token down
 * ([CoverSize.Rail]). The poster is never stretched to fill a leftover cell.
 */
class GridCoverSizeTest {

    @Test
    fun aPhoneFitsTwoFeaturedCovers() {
        assertEquals(CoverSize.Featured, gridCoverSize(360.dp))
        assertTrue(360.dp >= CoverSize.Featured.width * 2, "two columns of Featured must fit")
    }

    @Test
    fun aNarrowPhoneFallsBackToRail() {
        assertEquals(CoverSize.Rail, gridCoverSize(320.dp))
        assertTrue(320.dp >= CoverSize.Rail.width * 2, "two columns of Rail must fit")
    }

    @Test
    fun aTabletKeepsFeaturedWithMoreColumns() {
        assertEquals(CoverSize.Featured, gridCoverSize(600.dp))
        assertTrue(600.dp >= CoverSize.Featured.width * 3, "three columns of Featured must fit")
    }
}
