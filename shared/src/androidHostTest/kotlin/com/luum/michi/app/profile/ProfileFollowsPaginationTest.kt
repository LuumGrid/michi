package com.luum.michi.app.profile

import com.luum.michi.app.profile.domain.model.ProfileFollowsKind
import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.profile.ui.state.ProfileFollowsStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Following/followers pagination through the real repository + holder seam. */
class ProfileFollowsPaginationTest {

    private fun holder(client: FakeProfileGraphQL) = ProfileFollowsStateHolder(
        ProfileRepositoryImpl(client),
        CoroutineScope(Dispatchers.Unconfined),
    )

    @Test
    fun firstPageLoadsFollowing() {
        val client = FakeProfileGraphQL(listOf(followsPage("following", false, 1 to "A", 2 to "B")))
        val holder = holder(client)
        holder.load(7, ProfileFollowsKind.FOLLOWING)
        assertEquals(listOf("A", "B"), holder.follows.map { it.name })
        assertFalse(holder.hasNextPage)
        assertNull(holder.error)
    }

    @Test
    fun followersKindHitsTheOtherConnection() {
        val client = FakeProfileGraphQL(listOf(followsPage("followers", false, 1 to "A")))
        val holder = holder(client)
        holder.load(7, ProfileFollowsKind.FOLLOWERS)
        assertEquals(listOf("A"), holder.follows.map { it.name })
        assertTrue(client.requests.single().query.contains("followers(userId:"))
    }

    @Test
    fun loadMoreAppendsAndDeduplicates() {
        val client = FakeProfileGraphQL(
            listOf(
                followsPage("following", true, 1 to "A", 2 to "B"),
                followsPage("following", false, 2 to "B", 3 to "C"),
            ),
        )
        val holder = holder(client)
        holder.load(7, ProfileFollowsKind.FOLLOWING)
        assertTrue(holder.hasNextPage)
        holder.loadMore()
        assertEquals(listOf("A", "B", "C"), holder.follows.map { it.name })
        assertEquals(false, holder.hasNextPage)
    }

    @Test
    fun aFailedPageGoesToActionErrorNotTheBlockingError() {
        val client = FakeProfileGraphQL(
            listOf(followsPage("following", true, 1 to "A")),
            failureAt = 1,
        )
        val holder = holder(client)
        holder.load(7, ProfileFollowsKind.FOLLOWING)
        assertTrue(holder.hasNextPage)

        holder.loadMore()

        assertNull(holder.error)
        assertTrue(holder.actionError != null)
        holder.clearActionError()
        assertNull(holder.actionError)
    }

    @Test
    fun withinTheTtlReopeningCostsNoRequest() {
        val client = FakeProfileGraphQL(listOf(followsPage("following", false, 1 to "A")))
        val holder = holder(client)
        holder.load(7, ProfileFollowsKind.FOLLOWING)
        holder.load(7, ProfileFollowsKind.FOLLOWING)
        assertEquals(1, client.calls)
    }

    @Test
    fun switchingKindDropsThePreviousListAndRefetches() {
        val client = FakeProfileGraphQL(
            listOf(
                followsPage("following", false, 1 to "A"),
                followsPage("followers", false, 9 to "Z"),
            ),
        )
        val holder = holder(client)
        holder.load(7, ProfileFollowsKind.FOLLOWING)
        holder.load(7, ProfileFollowsKind.FOLLOWERS)
        assertEquals(2, client.calls)
        assertEquals(listOf("Z"), holder.follows.map { it.name })
    }

    @Test
    fun aFailedFirstPageSurfacesAndRetryRefetches() {
        val client = FakeProfileGraphQL(
            listOf(
                followsPage("following", false, 1 to "A"),
                followsPage("following", false, 1 to "A"),
            ),
            failureAt = 0,
        )
        val holder = holder(client)
        holder.load(7, ProfileFollowsKind.FOLLOWING)
        assertTrue(holder.error != null)

        holder.refresh()
        assertNull(holder.error)
        assertEquals(listOf("A"), holder.follows.map { it.name })
    }
}
