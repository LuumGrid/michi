package com.luum.michi.app.ui

import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.CoverSize
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The cover tokens are the app's only poster sizes: width and its 3:4 height
 * come from one place, so no surface can invent a size.
 */
class CoverSizeTest {

    @Test
    fun theStandardWidthsAreTheOnesWeChose() {
        assertEquals(111.dp, CoverSize.List.width)
        assertEquals(152.dp, CoverSize.Featured.width)
    }

    @Test
    fun theRailIsTheExactMidpointOfTheOtherTwo() {
        assertEquals((CoverSize.List.width + CoverSize.Featured.width) / 2, CoverSize.Rail.width)
        assertEquals(131.5.dp, CoverSize.Rail.width)
    }

    @Test
    fun theHeightAlwaysComesFromTheWidthAtThreeByFour() {
        // 111 * 4/3 is exact; the others are checked as a ratio (Float Dp).
        assertEquals(148.dp, CoverSize.List.height)
        listOf(CoverSize.List, CoverSize.Rail, CoverSize.Featured).forEach { size ->
            assertTrue(
                abs(size.height.value / size.width.value - 4f / 3f) < 0.001f,
                "$size is not 3:4",
            )
        }
    }
}
