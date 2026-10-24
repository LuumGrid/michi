package com.luum.michi.app.root

import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A genre page is a detail-stack layer carrying its genre + catalog. */
class StateGenreTest {

    private fun state() = State()

    @Test
    fun aGenrePushesOverTheMediaAndPopsBackToIt() {
        val state = state()
        state.openMedia(5)
        state.openGenre("Action", MediaType.MANGA)

        assertEquals(DetailDestination.Genre("Action", MediaType.MANGA), state.currentDetail)

        assertTrue(state.applyBackStep())
        assertEquals(DetailDestination.Media(5), state.currentDetail)
    }
}
