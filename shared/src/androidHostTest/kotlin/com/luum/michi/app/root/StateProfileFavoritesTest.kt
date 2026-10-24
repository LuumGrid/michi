package com.luum.michi.app.root

import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The favourites "View all" is a detail-stack layer, like a genre/catalog list. */
class StateProfileFavoritesTest {

    @Test
    fun aFavoritesGridPushesAndPops() {
        val state = State()

        state.openProfileFavorites("ANIME")
        assertEquals(DetailDestination.ProfileFavorites("ANIME"), state.currentDetail)

        assertTrue(state.applyBackStep())
        assertNull(state.currentDetail)
    }

    @Test
    fun openingAItemFromTheGridPushesOverIt() {
        val state = State()
        state.openProfileFavorites("CHARACTERS")
        state.openCharacter(42)

        assertEquals(DetailDestination.Character(42), state.currentDetail)

        assertTrue(state.applyBackStep())
        assertEquals(DetailDestination.ProfileFavorites("CHARACTERS"), state.currentDetail)
    }
}
