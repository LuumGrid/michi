package com.luum.michi.app.root

import com.luum.michi.app.root.state.BackStep
import com.luum.michi.app.root.state.RootOverlay
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The overlay set and its progressive back order (see [State.nextBackStep]). */
class StateOverlayTest {

    @Test
    fun backClosesEverySheetAtOnceBeforeSettings() {
        val state = State()
        state.open(RootOverlay.Settings)
        state.open(RootOverlay.SectionFilter)
        state.open(RootOverlay.ListSort)

        assertEquals(BackStep.CloseSheet, state.nextBackStep())
        assertTrue(state.applyBackStep())

        assertFalse(state.isOpen(RootOverlay.SectionFilter))
        assertFalse(state.isOpen(RootOverlay.ListSort))
        assertTrue(state.isOpen(RootOverlay.Settings), "every sheet closes before Settings")

        assertEquals(BackStep.CloseOverlay, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.isOpen(RootOverlay.Settings))
    }

    @Test
    fun backClosesSettingsAndSignInTogether() {
        val state = State()
        state.open(RootOverlay.Settings)
        state.open(RootOverlay.SignIn)

        assertEquals(BackStep.CloseOverlay, state.nextBackStep())
        assertTrue(state.applyBackStep())

        assertFalse(state.isOpen(RootOverlay.Settings))
        assertFalse(state.isOpen(RootOverlay.SignIn))
    }

    @Test
    fun detailClosesBeforeTheOverlayBelowIt() {
        val state = State()
        state.openMedia(1)
        state.open(RootOverlay.Calendar)

        assertEquals(BackStep.CloseDetail, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.isDetailOpen)
        assertTrue(state.isOpen(RootOverlay.Calendar), "the detail closes before the overlay below")

        assertEquals(BackStep.CloseOverlay, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.isOpen(RootOverlay.Calendar))
    }

    @Test
    fun resetClosesEveryOverlay() {
        val state = State()
        state.open(RootOverlay.Settings)
        state.open(RootOverlay.Calendar)
        state.open(RootOverlay.SectionFilter)
        state.openMedia(1)

        state.reset()

        assertTrue(state.openOverlays.isEmpty())
        assertFalse(state.isDetailOpen)
    }

    @Test
    fun closingAnOverlayThatIsNotOpenChangesNothing() {
        val state = State()
        state.open(RootOverlay.Settings)
        val before = state.openOverlays

        state.close(RootOverlay.Calendar)

        assertEquals(before, state.openOverlays)
        assertTrue(state.isOpen(RootOverlay.Settings))
        assertFalse(state.isOpen(RootOverlay.Calendar))
    }
}
