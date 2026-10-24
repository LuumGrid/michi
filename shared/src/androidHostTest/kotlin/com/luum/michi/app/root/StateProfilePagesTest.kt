package com.luum.michi.app.root

import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.profile.domain.model.ProfileFollow
import com.luum.michi.app.profile.domain.model.ProfileFollowsKind
import com.luum.michi.app.profile.ui.model.aniListUrl
import com.luum.michi.app.root.state.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The statistics/follows/reviews pages are detail-stack layers, like favourites. */
class StateProfilePagesTest {

    @Test
    fun statisticsPushesSeededOnTheTappedType() {
        val state = State()
        state.openProfileStatistics(MediaType.MANGA)
        assertEquals(DetailDestination.ProfileStatistics(MediaType.MANGA), state.currentDetail)
        assertTrue(state.applyBackStep())
        assertNull(state.currentDetail)
    }

    @Test
    fun followsPushesAndPops() {
        val state = State()
        state.openProfileFollows(ProfileFollowsKind.FOLLOWERS.name)
        assertEquals(DetailDestination.ProfileFollows("FOLLOWERS"), state.currentDetail)
        assertTrue(state.applyBackStep())
        assertNull(state.currentDetail)
    }

    @Test
    fun reviewsPushesAndBackClosesIt() {
        val state = State()
        state.openProfileReviews()
        assertEquals(DetailDestination.ProfileReviews, state.currentDetail)
        assertTrue(state.applyBackStep())
        assertNull(state.currentDetail)
    }

    @Test
    fun aReviewOpensItsMediaOverThePage() {
        val state = State()
        state.openProfileReviews()
        state.openMedia(42)
        assertEquals(DetailDestination.Media(42), state.currentDetail)
        assertTrue(state.applyBackStep())
        assertEquals(DetailDestination.ProfileReviews, state.currentDetail)
    }

    @Test
    fun aFollowBuildsTheAniListProfileUrl() {
        assertEquals(
            "https://anilist.co/user/alice",
            ProfileFollow(id = 1, name = "alice", avatar = null).aniListUrl(),
        )
    }
}
