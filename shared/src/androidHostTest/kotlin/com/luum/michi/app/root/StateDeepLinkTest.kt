package com.luum.michi.app.root

import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.root.state.RootOverlay
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateDeepLinkTest {

    @Test
    fun openingADeepLinkClosesSettingsAndStacksTheDestination() {
        val state = State()
        state.open(RootOverlay.Settings)
        assertTrue(state.isOpen(RootOverlay.Settings))

        state.openDeepLink(DetailDestination.Character(5))

        assertFalse(state.isOpen(RootOverlay.Settings), "the detail must not stay hidden behind Settings")
        assertTrue(state.isDetailOpen)
        assertEquals(DetailDestination.Character(5), state.currentDetail)
    }

    @Test
    fun openingTheSameDestinationTwiceDoesNotDuplicate() {
        val state = State()

        state.openDeepLink(DetailDestination.Staff(4))
        state.openDeepLink(DetailDestination.Staff(4))

        assertEquals(DetailDestination.Staff(4), state.currentDetail)
        assertEquals(1, state.detailEntryIds.size)
    }
}
