package com.luum.michi.app.root

import com.luum.michi.app.root.state.BackStep
import com.luum.michi.app.root.state.RootOverlay
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The About page is a flag inside Settings: back leaves it before Settings. */
class StateSettingsAboutTest {

    private fun openAbout(): State = State().apply {
        open(RootOverlay.Settings)
        openSettingsAbout()
    }

    @Test
    fun backLeavesAboutBeforeClosingSettings() {
        val state = openAbout()

        assertEquals(BackStep.CloseSettingsAbout, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.settingsAboutOpen)
        assertTrue(state.isOpen(RootOverlay.Settings), "Settings stays open")

        assertEquals(BackStep.CloseOverlay, state.nextBackStep(), "the next back closes Settings")
        assertTrue(state.applyBackStep())
        assertFalse(state.isOpen(RootOverlay.Settings))
    }

    @Test
    fun signInClosesFirstInsteadOfLeavingAbout() {
        val state = openAbout()
        state.open(RootOverlay.SignIn)

        assertEquals(BackStep.CloseOverlay, state.nextBackStep())
    }

    @Test
    fun reopeningSettingsStartsOnTheList() {
        val state = openAbout()

        state.close(RootOverlay.Settings)
        state.open(RootOverlay.Settings)

        assertFalse(state.settingsAboutOpen)
    }

    @Test
    fun resetClosesAbout() {
        val state = openAbout()

        state.reset()

        assertFalse(state.settingsAboutOpen)
    }
}
