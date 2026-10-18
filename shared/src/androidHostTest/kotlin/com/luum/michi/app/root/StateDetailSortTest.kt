package com.luum.michi.app.root

import com.luum.michi.app.root.state.BackStep
import com.luum.michi.app.root.state.RootOverlay
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun newState() = State()

class StateDetailSortTest {

    @Test
    fun detailSortClosesBeforeThePageItSitsOn() {
        val state = newState()
        state.openStudio(5)
        state.open(RootOverlay.DetailSort)

        assertEquals(BackStep.CloseSheet, state.nextBackStep())
        assertTrue(state.applyBackStep())

        assertFalse(state.isOpen(RootOverlay.DetailSort))
        assertTrue(state.isDetailOpen, "the first back only closes the sheet")
        assertEquals(BackStep.CloseDetail, state.nextBackStep())
    }

    @Test
    fun leavingOrReplacingTheDetailDropsTheSortFlag() {
        val state = newState()
        state.openStudio(5)
        state.open(RootOverlay.DetailSort)
        state.openMedia(9)
        assertFalse(state.isOpen(RootOverlay.DetailSort))

        state.openStudio(6)
        state.open(RootOverlay.DetailSort)
        state.closeDetail()
        assertFalse(state.isOpen(RootOverlay.DetailSort))
    }
}
