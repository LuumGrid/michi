package com.luum.michi.app.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.CoverSize
import com.luum.michi.app.ui.components.gridCoverSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val MinGap = 12.dp
private const val MaxGrowth = 1.25f

/** The documented base rule, re-stated here so the tests pin the token choice. */
private fun baseFor(width: Dp, minColumns: Int = 2): CoverSize =
    if (width >= CoverSize.Featured.width * minColumns + MinGap * (minColumns + 1)) {
        CoverSize.Featured
    } else {
        CoverSize.Rail
    }

/**
 * A poster grid sizes its cells from a standard token — [CoverSize.Featured]
 * while at least two of them fit with air, otherwise [CoverSize.Rail] — and then
 * grows the cell to fill the width, never past +25% (`GridMaxGrowth`). It never
 * goes below the base token and never stretches past the cap: the leftover after
 * the cap is spread as even air.
 */
class GridCoverSizeTest {

    @Test
    fun aPhoneGrowsTwoColumnsToFillTheWidth() {
        val size = gridCoverSize(360.dp)

        assertEquals(CoverSize.Featured, baseFor(360.dp))
        assertEquals(162.dp, size.width)
        assertTrue(size.width >= CoverSize.Featured.width, "never below the base")
        assertTrue(size.width <= CoverSize.Featured.width * MaxGrowth, "never past the cap")
        assertEquals(size.width * 4 / 3, size.height, "the height stays 3:4")
    }

    @Test
    fun aWiderPhoneGrowsMoreButStaysInsideTheRange() {
        val size = gridCoverSize(411.dp)

        assertEquals(CoverSize.Featured, baseFor(411.dp))
        assertEquals(187.5.dp, size.width)
        assertTrue(size.width in CoverSize.Featured.width..(CoverSize.Featured.width * MaxGrowth))
    }

    @Test
    fun aNarrowPhoneFallsBackToRailAndGrowsIt() {
        val size = gridCoverSize(320.dp)

        assertEquals(CoverSize.Rail, baseFor(320.dp))
        assertEquals(142.dp, size.width)
        assertTrue(size.width in CoverSize.Rail.width..(CoverSize.Rail.width * MaxGrowth))
    }

    @Test
    fun aTabletUsesMoreColumnsAndStaysInsideTheRange() {
        val size = gridCoverSize(600.dp)

        assertEquals(CoverSize.Featured, baseFor(600.dp))
        // 184dp is under the 190dp cap, which only a 2-column layout would hit:
        // the width proves a third column is in play.
        assertEquals(184.dp, size.width)
        assertTrue(size.width < CoverSize.Featured.width * MaxGrowth)
    }

    @Test
    fun everyWidthStaysBetweenTheBaseTokenAndTheCap() {
        generateSequence(300) { it + 7 }
            .takeWhile { it <= 1200 }
            .forEach { widthDp ->
                val width = widthDp.dp
                val base = baseFor(width)
                val size = gridCoverSize(width)
                assertTrue(size.width >= base.width, "$widthDp: below the base token")
                assertTrue(size.width <= base.width * MaxGrowth, "$widthDp: past the cap")
                assertEquals(size.width * 4 / 3, size.height, "$widthDp: not 3:4")
            }
    }
}
