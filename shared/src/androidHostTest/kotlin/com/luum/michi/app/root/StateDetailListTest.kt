package com.luum.michi.app.root

import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.profile.domain.model.ProfileDraft
import com.luum.michi.app.root.state.BackStep
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun stateForLists() = State(
    initialProfile = ProfileDraft(
        username = "",
        displayName = "",
        avatarUrl = null,
        bannerUrl = null,
        bio = "",
    ),
)

/**
 * The "View all" list is a detail-stack layer of its own: it pushes over its
 * parent page and a single back pops just the list (`Root` then re-binds the
 * parent's holder from [DetailDestination.DetailList.parentId]).
 */
class StateDetailListTest {

    @Test
    fun aViewAllListPushesOverItsParentAndPopsBackToIt() {
        val state = stateForLists()
        state.openMedia(5)
        state.openDetailList(DetailListKind.MEDIA_RELATED, parentId = 5)

        assertEquals(
            DetailDestination.DetailList(DetailListKind.MEDIA_RELATED, 5),
            state.currentDetail,
        )
        assertEquals(BackStep.CloseDetail, state.nextBackStep())

        assertTrue(state.applyBackStep())
        assertEquals(DetailDestination.Media(5), state.currentDetail)

        // A second back leaves the detail entirely.
        assertTrue(state.applyBackStep())
        assertNull(state.currentDetail)
    }

    @Test
    fun reopeningTheSameListDoesNotDoublePush() {
        val state = stateForLists()
        state.openMedia(5)
        state.openDetailList(DetailListKind.MEDIA_RELATED, 5)
        state.openDetailList(DetailListKind.MEDIA_RELATED, 5)

        assertTrue(state.applyBackStep())
        assertEquals(DetailDestination.Media(5), state.currentDetail)
    }
}
