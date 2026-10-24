package com.luum.michi.app.root

import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.root.state.BackStep
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The favourites "View all" page's edit mode: back leaves it before the page. */
class StateFavoritesEditTest {

    private fun editingPage(): State = State().apply {
        openProfileFavorites("ANIME")
        toggleFavoritesEditing()
    }

    @Test
    fun backExitsTheEditModeBeforeClosingThePage() {
        val state = editingPage()

        assertEquals(BackStep.CloseFavoritesEdit, state.nextBackStep())
        assertTrue(state.applyBackStep())
        assertFalse(state.favoritesEditing)
        assertEquals(
            DetailDestination.ProfileFavorites("ANIME"),
            state.currentDetail,
            "the page stays open",
        )

        assertTrue(state.applyBackStep(), "a second back closes the page")
        assertEquals(null, state.currentDetail)
    }

    @Test
    fun leavingThePageExitsTheEditMode() {
        val state = editingPage()

        state.closeDetail()

        assertFalse(state.favoritesEditing)
        assertEquals(null, state.currentDetail)
    }

    @Test
    fun changingTabExitsTheEditMode() {
        val state = editingPage()

        state.selectTab(TabSection.ANIME)

        assertFalse(state.favoritesEditing)
    }

    @Test
    fun resetExitsTheEditMode() {
        val state = editingPage()

        state.reset()

        assertFalse(state.favoritesEditing)
    }

    @Test
    fun theEditModeNeverEatsBackOutsideItsPage() {
        val state = State()
        state.toggleFavoritesEditing()

        assertFalse(state.favoritesEditing && state.currentDetail is DetailDestination.ProfileFavorites)
        assertFalse(state.applyBackStep(), "with no page open, back falls through to the system")
    }
}
