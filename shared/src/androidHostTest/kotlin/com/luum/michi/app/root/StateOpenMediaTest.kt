package com.luum.michi.app.root

import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.root.state.RootOverlay
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A deep link can open a media detail while Settings is showing. The detail is
 * content-routed below Settings, so opening it must close Settings or the detail
 * stays hidden behind an empty-titled overlay.
 */
class StateOpenMediaTest {

    private fun state() = State()

    @Test
    fun openingMediaClosesSettingsAndStacksTheDetail() {
        val state = state()
        state.open(RootOverlay.Settings)
        assertTrue(state.isOpen(RootOverlay.Settings))

        state.openMedia(1)

        assertFalse(state.isOpen(RootOverlay.Settings), "the detail must not stay hidden behind Settings")
        assertTrue(state.isDetailOpen)
        assertEquals(DetailDestination.Media(1), state.currentDetail)
    }

    @Test
    fun openingMediaWithSettingsClosedIsUnaffected() {
        val state = state()

        state.openMedia(7)

        assertFalse(state.isOpen(RootOverlay.Settings))
        assertEquals(DetailDestination.Media(7), state.currentDetail)
    }
}
