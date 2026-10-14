package com.luum.michi.app.root

import com.luum.michi.app.profile.domain.model.ProfileDraft
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Per-entry identity for the detail stack. Root keys the saved scroll state on
 * it, so each entry starts at the top, back restores that entry's position and
 * re-pushing the same media is a different entry.
 */
class StateDetailEntryTest {

    private fun state() = State(
        initialProfile = ProfileDraft(
            username = "",
            displayName = "",
            avatarUrl = null,
            bannerUrl = null,
            bio = "",
        ),
    )

    @Test
    fun eachPushGetsItsOwnEntryIdentity() {
        val state = state()
        assertEquals(0L, state.currentDetailEntryId)

        state.openMedia(1)
        val first = state.currentDetailEntryId
        state.openMedia(2)
        val second = state.currentDetailEntryId

        assertNotEquals(first, second)
        assertEquals(listOf(first, second), state.detailEntryIds)
    }

    @Test
    fun poppingDropsTheEntry() {
        val state = state()
        state.openMedia(1)
        val first = state.currentDetailEntryId
        state.openMedia(2)

        state.closeDetail()

        assertEquals(listOf(first), state.detailEntryIds)
        assertEquals(first, state.currentDetailEntryId)

        state.closeDetail()

        assertEquals(0L, state.currentDetailEntryId)
        assertTrue(state.detailEntryIds.isEmpty())
    }

    @Test
    fun rePushingTheSameMediaIsADifferentEntry() {
        val state = state()
        state.openMedia(5)
        val first = state.currentDetailEntryId
        state.openCharacter(9)

        state.openMedia(5) // A -> B -> A: the stack holds A twice

        assertNotEquals(first, state.currentDetailEntryId)
        assertEquals(3, state.detailEntryIds.size)
        assertEquals(3, state.detailEntryIds.distinct().size)
    }

    @Test
    fun theSameDestinationOnTopIsNotANewEntry() {
        val state = state()
        state.openMedia(5)
        val first = state.currentDetailEntryId

        state.openMedia(5)

        assertEquals(first, state.currentDetailEntryId)
        assertEquals(1, state.detailEntryIds.size)
    }
}
