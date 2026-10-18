package com.luum.michi.app.root

import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.root.state.BackStep
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** PROFILE's favourites edit mode: a tab-surface state with a back step. */
class StateProfileEditTest {

    @Test
    fun togglingInAndOutOfEditMode() {
        val state = State()

        assertFalse(state.profileEditing)
        state.toggleProfileEditing()
        assertTrue(state.profileEditing)
        state.toggleProfileEditing()
        assertFalse(state.profileEditing)
    }

    @Test
    fun backExitsEditMode() {
        val state = State()
        state.toggleProfileEditing()

        assertTrue(state.nextBackStep() == BackStep.CloseProfileEdit)
        assertTrue(state.applyBackStep())
        assertFalse(state.profileEditing)
    }

    @Test
    fun changingTabExitsEditMode() {
        val state = State()
        state.toggleProfileEditing()

        state.selectTab(TabSection.ANIME)

        assertFalse(state.profileEditing)
    }

    @Test
    fun resetExitsEditMode() {
        val state = State()
        state.toggleProfileEditing()

        state.reset()

        assertFalse(state.profileEditing)
    }
}
