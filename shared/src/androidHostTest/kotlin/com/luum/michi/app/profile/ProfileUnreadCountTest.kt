package com.luum.michi.app.profile

import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.profile.ui.state.ProfileStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

private fun holderForwardingCount(graphQL: FakeProfileGraphQL, received: MutableList<Int>) =
    ProfileStateHolder(
        repository = ProfileRepositoryImpl(graphQL),
        favouriteRepository = FakeFavouriteRepository(),
        scope = CoroutineScope(Dispatchers.Unconfined),
        onUnreadNotificationCount = { received += it },
    )

/**
 * The notifications dot's single feeder: the profile forwards the Viewer's
 * unread count (a free second root field on the same request) and keeps no copy.
 */
class ProfileUnreadCountTest {

    @Test
    fun theProfileForwardsTheViewerUnreadCount() {
        val received = mutableListOf<Int>()
        val holder = holderForwardingCount(
            FakeProfileGraphQL(listOf(profileJson(anime = listOf(1 to "A"), unreadCount = 4))),
            received,
        )

        holder.load(7)

        assertEquals(listOf(4), received)
    }

    @Test
    fun aMissingCountIsNotForwarded() {
        val received = mutableListOf<Int>()
        val holder = holderForwardingCount(
            FakeProfileGraphQL(listOf(profileJson(anime = listOf(1 to "A")))),
            received,
        )

        holder.load(7)

        assertEquals(emptyList(), received, "a null count must never blank the dot")
    }
}
