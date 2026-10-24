package com.luum.michi.app.ui

import com.luum.michi.app.ui.components.cardRailVisibleRows
import kotlin.test.Test
import kotlin.test.assertEquals

/** A card rail never reserves more rows than its items fill. */
class CardRailVisibleRowsTest {

    @Test
    fun theVisibleRowsFollowTheItemCappedAtThree() {
        assertEquals(1, cardRailVisibleRows(0, 3))
        assertEquals(1, cardRailVisibleRows(1, 3))
        assertEquals(2, cardRailVisibleRows(2, 3))
        assertEquals(3, cardRailVisibleRows(3, 3))
        assertEquals(3, cardRailVisibleRows(10, 3))
    }

    @Test
    fun aSingleRowRailReservesOne() {
        assertEquals(1, cardRailVisibleRows(1, 1))
    }
}
