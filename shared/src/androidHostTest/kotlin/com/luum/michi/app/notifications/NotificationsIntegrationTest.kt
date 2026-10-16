package com.luum.michi.app.notifications

import com.luum.michi.app.DeferredDispatcher
import com.luum.michi.app.FakeGraphQL
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.notifications.domain.model.NotificationCategory
import com.luum.michi.app.notifications.domain.model.NotificationFilter
import com.luum.michi.app.notifications.domain.model.NotificationTarget
import com.luum.michi.app.notifications.repository.NotificationsRepositoryImpl
import com.luum.michi.app.notifications.ui.state.NotificationsStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** One notification node as the API shapes it (only the fields a test needs). */
private fun node(
    id: Int,
    type: String,
    activityId: Int? = null,
    commentId: Int? = null,
    threadId: Int? = null,
    thread: Int? = null,
    userName: String? = null,
): JsonObject = buildJsonObject {
    put("id", id)
    put("type", type)
    if (activityId != null) put("activityId", activityId)
    if (commentId != null) put("commentId", commentId)
    if (threadId != null) put("threadId", threadId)
    if (thread != null) put("thread", buildJsonObject { put("id", thread) })
    if (userName != null) {
        put("user", buildJsonObject {
            put("id", id)
            put("name", userName)
            put("avatar", buildJsonObject { put("large", "https://img/$id.jpg") })
        })
    }
    put("createdAt", 1_700_000_000L)
}

private fun notificationPage(
    notifications: List<JsonObject> = emptyList(),
    hasNextPage: Boolean = false,
    unreadCount: Int? = null,
): JsonElement = buildJsonObject {
    if (unreadCount != null) {
        put("Viewer", buildJsonObject { put("unreadNotificationCount", unreadCount) })
    }
    put("Page", buildJsonObject {
        put("pageInfo", buildJsonObject {
            put("hasNextPage", hasNextPage)
            put("currentPage", 1)
        })
        put("notifications", JsonArray(notifications.toList()))
    })
}

private fun idsOf(holder: NotificationsStateHolder) = holder.notifications.map { it.id }

private fun webUrl(target: NotificationTarget): String? = (target as? NotificationTarget.Web)?.url

private fun withCount(request: AniListGraphQLRequest): Boolean? =
    (request.variables?.get("withCount") as? JsonPrimitive)?.booleanOrNull

private fun resetFlag(request: AniListGraphQLRequest): Boolean? =
    (request.variables?.get("reset") as? JsonPrimitive)?.booleanOrNull

/**
 * Notifications at the repository seam: a scripted [FakeGraphQL] (canned JSON)
 * behind the real repository and holder. Race tests drive [DeferredDispatcher]
 * so a page/clear/cancel order is deterministic, and set [FakeGraphQL.gate] to
 * hold requests in flight.
 */
class NotificationsIntegrationTest {

    // ────────────── query shape ──────────────

    @Test
    fun queryAsksTheCountOnPageOneAndCarriesTheMissingFragments() = runBlocking {
        val client = FakeGraphQL(listOf(
            notificationPage(listOf(node(1, "AIRING")), hasNextPage = true, unreadCount = 3),
            notificationPage(listOf(node(2, "AIRING"))),
        ))
        val repository = NotificationsRepositoryImpl(client)

        repository.loadNotifications(page = 1, resetCount = false, filter = NotificationFilter.ALL)
        repository.loadNotifications(page = 2, resetCount = false, filter = NotificationFilter.ALL)

        val first = client.requests[0]
        assertEquals(true, withCount(first), "page 1 always asks the unread count")
        assertEquals(false, resetFlag(first))
        listOf(
            "ActivityReplySubscribedNotification",
            "ThreadCommentSubscribedNotification",
            "ThreadCommentLikeNotification",
            "ThreadLikeNotification",
        ).forEach { assertTrue(first.query.contains("... on $it"), "missing fragment for $it") }
        assertTrue(first.query.contains("commentId"), "forum fragments need commentId")
        assertTrue(first.query.contains("threadId"), "ThreadLikeNotification needs threadId")

        assertEquals(false, withCount(client.requests[1]), "later pages do not ask the count")
    }

    // ────────────── mapper targets ──────────────

    @Test
    fun mapsEachTargetFamilyAndFallsBackToTheFeed() = runBlocking {
        val client = FakeGraphQL(listOf(
            notificationPage(listOf(
                node(1, "ACTIVITY_LIKE", activityId = 5),
                node(2, "THREAD_LIKE", threadId = 9),
                node(3, "THREAD_COMMENT_REPLY", thread = 9, commentId = 3),
                node(4, "FOLLOWING", userName = "bob"),
                node(5, "MEDIA_DELETION"),
            )),
        ))
        val page = successValue(NotificationsRepositoryImpl(client).loadNotifications(1, false, NotificationFilter.ALL))
        val byId = page.notifications.associateBy { it.id }

        assertEquals("https://anilist.co/activity/5", webUrl(byId.getValue(1).target))
        assertEquals("https://anilist.co/forum/thread/9", webUrl(byId.getValue(2).target))
        assertEquals("https://anilist.co/forum/thread/9/comment/3", webUrl(byId.getValue(3).target))
        assertEquals("https://anilist.co/user/bob", webUrl(byId.getValue(4).target))
        assertEquals("https://anilist.co/notifications", webUrl(byId.getValue(5).target))
    }

    @Test
    fun theFourPreviouslyMissingTypesAreMapped() = runBlocking {
        val client = FakeGraphQL(listOf(
            notificationPage(listOf(
                node(1, "ACTIVITY_REPLY_SUBSCRIBED", activityId = 11),
                node(2, "THREAD_SUBSCRIBED", thread = 7, commentId = 21),
                node(3, "THREAD_COMMENT_LIKE", thread = 7, commentId = 22),
                node(4, "THREAD_LIKE", threadId = 8),
            )),
        ))
        val page = successValue(NotificationsRepositoryImpl(client).loadNotifications(1, false, NotificationFilter.ALL))
        val byId = page.notifications.associateBy { it.id }

        assertEquals(4, page.notifications.size, "none of the four is dropped")
        assertEquals(NotificationCategory.ACTIVITY, byId.getValue(1).category)
        assertEquals("https://anilist.co/activity/11", webUrl(byId.getValue(1).target))
        assertEquals(NotificationCategory.FORUM, byId.getValue(2).category)
        assertEquals("https://anilist.co/forum/thread/7/comment/21", webUrl(byId.getValue(2).target))
        assertEquals("https://anilist.co/forum/thread/7/comment/22", webUrl(byId.getValue(3).target))
        assertEquals("https://anilist.co/forum/thread/8", webUrl(byId.getValue(4).target))
    }

    // ────────────── holder: unread count ──────────────

    @Test
    fun unreadCountComesFromPageOneAndIsForcedToZeroOnReset() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeGraphQL(listOf(
                notificationPage(listOf(node(1, "AIRING")), unreadCount = 7),
                notificationPage(listOf(node(1, "AIRING")), unreadCount = 9),
            ))
            val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), scope)

            holder.load()
            dispatcher.runAll()
            assertEquals(7, holder.unreadCount, "page 1 reports the count without a reset")

            holder.load(resetCount = true)
            dispatcher.runAll()
            assertEquals(0, holder.unreadCount, "a reset forces 0, ignoring the returned value")

            assertEquals(true, resetFlag(client.requests[1]))
        } finally {
            scope.cancel()
        }
    }

    // ────────────── holder: filter ──────────────

    @Test
    fun selectFilterReloadsWithTheNewFilterAndIgnoresTheSameOne() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeGraphQL(listOf(
                notificationPage(listOf(node(1, "AIRING"))),
                notificationPage(listOf(node(5, "THREAD_LIKE"))),
            ))
            val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), scope)

            holder.load()
            dispatcher.runAll()
            assertEquals(1, client.calls)

            holder.selectFilter(NotificationFilter.ALL) // already the selected filter
            dispatcher.runAll()
            assertEquals(1, client.calls, "the same filter does not reload")

            holder.selectFilter(NotificationFilter.FORUM)
            dispatcher.runAll()
            assertEquals(2, client.calls)
            assertEquals(NotificationFilter.FORUM, holder.selectedFilter)
            assertTrue(
                client.requests[1].variables?.get("types").toString().contains("THREAD_LIKE"),
                "the new filter is sent",
            )
        } finally {
            scope.cancel()
        }
    }

    // ────────────── holder: races ──────────────

    @Test
    fun aReloadDropsAnInFlightLoadMore() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeGraphQL(listOf(
                notificationPage(listOf(node(1, "AIRING")), hasNextPage = true),
                notificationPage(listOf(node(99, "AIRING"))), // the stale page 2
                notificationPage(listOf(node(2, "AIRING"))), // the reloaded page 1
            ))
            val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), scope)

            holder.load()
            dispatcher.runAll()
            assertEquals(listOf(1), idsOf(holder))

            client.gate = CompletableDeferred()
            holder.loadMore()
            dispatcher.runAll()
            assertTrue(holder.isLoadingMore)

            holder.load()
            dispatcher.runAll()
            assertFalse(holder.isLoadingMore, "the reload drops the page in flight")
            assertEquals(listOf(1), idsOf(holder), "items stay until page 1 lands")

            client.gate!!.complete(Unit)
            dispatcher.runAll()
            assertEquals(listOf(2), idsOf(holder), "the stale page 2 must not be appended")
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aCancelledLoadDoesNotStopTheCurrentSpinner() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeGraphQL(listOf(
                notificationPage(listOf(node(2, "AIRING"))),
                notificationPage(listOf(node(2, "AIRING"))),
            ))
            val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), scope)

            client.gate = CompletableDeferred()
            holder.load()
            dispatcher.runAll()
            assertTrue(holder.isLoading)

            holder.load()
            dispatcher.runAll()
            assertTrue(holder.isLoading, "the stale finally must not stop the current spinner")

            client.gate!!.complete(Unit)
            dispatcher.runAll()
            assertFalse(holder.isLoading)
            assertEquals(listOf(2), idsOf(holder))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aRefreshKeepsTheItemsUntilPageOneArrives() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeGraphQL(listOf(
                notificationPage(listOf(node(1, "AIRING"))),
                notificationPage(listOf(node(2, "AIRING"))),
            ))
            val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), scope)

            holder.load()
            dispatcher.runAll()
            assertEquals(listOf(1), idsOf(holder))

            client.gate = CompletableDeferred()
            holder.load()
            dispatcher.runAll()
            assertTrue(holder.isLoading)
            assertEquals(listOf(1), idsOf(holder), "a refresh keeps the loaded items visible")

            client.gate!!.complete(Unit)
            dispatcher.runAll()
            assertEquals(listOf(2), idsOf(holder))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun selectFilterEmptiesTheListAtOnce() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeGraphQL(listOf(
                notificationPage(listOf(node(1, "AIRING"))),
                notificationPage(listOf(node(5, "THREAD_LIKE"))),
            ))
            val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), scope)

            holder.load()
            dispatcher.runAll()
            assertEquals(listOf(1), idsOf(holder))

            client.gate = CompletableDeferred()
            holder.selectFilter(NotificationFilter.FORUM)
            assertTrue(holder.notifications.isEmpty(), "the old filter's items are dropped at once")
            assertTrue(holder.isLoading)

            client.gate!!.complete(Unit)
            dispatcher.runAll()
            assertEquals(listOf(5), idsOf(holder))
        } finally {
            scope.cancel()
        }
    }

    private fun <T> successValue(result: NetworkResult<T>): T = when (result) {
        is NetworkResult.Success -> result.value
        is NetworkResult.Failure -> throw AssertionError("expected Success but was $result")
    }
}
