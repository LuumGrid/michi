package com.luum.michi.app.root

import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.root.state.RootOverlay
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The notifications page is a BELOW_DETAIL overlay: a media detail opens over it. */
class StateNotificationsTest {

    @Test
    fun aMediaNotificationOpensTheDetailOverThePageAndBackReturnsToIt() {
        val state = State()
        state.open(RootOverlay.Notifications)
        assertTrue(state.isOpen(RootOverlay.Notifications))

        state.openMedia(5)
        assertEquals(DetailDestination.Media(5), state.currentDetail)

        assertTrue(state.applyBackStep(), "back closes the detail first")
        assertNull(state.currentDetail)
        assertTrue(
            state.isOpen(RootOverlay.Notifications),
            "the notifications page survives the detail opened from it",
        )

        assertTrue(state.applyBackStep(), "then back closes the page")
        assertFalse(state.isOpen(RootOverlay.Notifications))
    }

    @Test
    fun backClosesThePage() {
        val state = State()
        state.open(RootOverlay.Notifications)

        assertTrue(state.applyBackStep())
        assertFalse(state.isOpen(RootOverlay.Notifications))
    }
}
