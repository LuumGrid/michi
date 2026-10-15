package com.luum.michi.app.root

import com.luum.michi.app.profile.domain.model.ProfileDraft
import com.luum.michi.app.root.state.BackStep
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun newState() = State(
    initialProfile = ProfileDraft(
        username = "",
        displayName = "",
        avatarUrl = null,
        bannerUrl = null,
        bio = "",
    ),
)

class StateDetailSortTest {

    @Test
    fun detailSortClosesBeforeThePageItSitsOn() {
        val state = newState()
        state.openStudio(5)
        state.openDetailSort()

        assertEquals(BackStep.CloseSheet, state.nextBackStep())
        assertTrue(state.applyBackStep())

        assertFalse(state.isDetailSortOpen)
        assertTrue(state.isDetailOpen, "the first back only closes the sheet")
        assertEquals(BackStep.CloseDetail, state.nextBackStep())
    }

    @Test
    fun leavingOrReplacingTheDetailDropsTheSortFlag() {
        val state = newState()
        state.openStudio(5)
        state.openDetailSort()
        state.openMedia(9)
        assertFalse(state.isDetailSortOpen)

        state.openStudio(6)
        state.openDetailSort()
        state.closeDetail()
        assertFalse(state.isDetailSortOpen)
    }
}
