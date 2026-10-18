package com.luum.michi.app.notifications

import com.luum.michi.app.DeferredDispatcher
import com.luum.michi.app.FakeGraphQL
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.notifications.domain.model.NotificationFilter
import com.luum.michi.app.notifications.domain.model.NotificationKind
import com.luum.michi.app.notifications.domain.model.NotificationTarget
import com.luum.michi.app.notifications.domain.model.kinds
import com.luum.michi.app.notifications.repository.NotificationsRepositoryImpl
import com.luum.michi.app.notifications.repository.toAniListTypes
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** One notification node as the API shapes it (only the fields a test needs). */
private fun node(
    id: Int,
    type: String,
    activityId: Int? = null,
    commentId: Int? = null,
    threadId: Int? = null,
    thread: Int? = null,
    threadTitle: String? = null,
    userName: String? = null,
    mediaId: Int? = null,
    deletedMediaTitle: String? = null,
    deletedMediaTitles: List<String> = emptyList(),
    status: String? = null,
    notes: String? = null,
    submittedTitle: String? = null,
    staffName: String? = null,
    characterName: String? = null,
): JsonObject = buildJsonObject {
    put("id", id)
    put("type", type)
    if (activityId != null) put("activityId", activityId)
    if (commentId != null) put("commentId", commentId)
    if (threadId != null) put("threadId", threadId)
    if (thread != null || threadTitle != null) {
        put("thread", buildJsonObject {
            if (thread != null) put("id", thread)
            if (threadTitle != null) put("title", threadTitle)
        })
    }
    if (userName != null) {
        put("user", buildJsonObject {
            put("id", id)
            put("name", userName)
            put("avatar", buildJsonObject { put("large", "https://img/$id.jpg") })
        })
    }
    if (mediaId != null) {
        put("media", buildJsonObject {
            put("id", mediaId)
            put("title", buildJsonObject { put("userPreferred", "Media $mediaId") })
            put("coverImage", buildJsonObject { put("large", "https://img/media$mediaId.jpg") })
        })
    }
    if (deletedMediaTitle != null) put("deletedMediaTitle", deletedMediaTitle)
    if (deletedMediaTitles.isNotEmpty()) {
        put("deletedMediaTitles", JsonArray(deletedMediaTitles.map { JsonPrimitive(it) }))
    }
    if (status != null) put("status", status)
    if (notes != null) put("notes", notes)
    if (submittedTitle != null) put("submittedTitle", submittedTitle)
    if (staffName != null) {
        put("staff", buildJsonObject {
            put("id", id)
            put("name", buildJsonObject { put("userPreferred", staffName) })
            put("image", buildJsonObject { put("large", "https://img/staff$id.jpg") })
        })
    }
    if (characterName != null) {
        put("character", buildJsonObject {
            put("id", id)
            put("name", buildJsonObject { put("userPreferred", characterName) })
            put("image", buildJsonObject { put("large", "https://img/char$id.jpg") })
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

private fun unconfined() = CoroutineScope(Dispatchers.Unconfined)

/**
 * Notifications at the repository seam: a scripted [FakeGraphQL] (canned JSON)
 * behind the real repository and holder. Race tests drive [DeferredDispatcher]
 * so a page/clear/cancel order is deterministic, and set [FakeGraphQL.gate] to
 * hold requests in flight.
 */
class NotificationsIntegrationTest {

    private val allKinds = listOf(
        "AIRING" to NotificationKind.AIRING,
        "FOLLOWING" to NotificationKind.FOLLOWING,
        "ACTIVITY_LIKE" to NotificationKind.ACTIVITY_LIKE,
        "ACTIVITY_REPLY" to NotificationKind.ACTIVITY_REPLY,
        "ACTIVITY_REPLY_LIKE" to NotificationKind.ACTIVITY_REPLY_LIKE,
        "ACTIVITY_REPLY_SUBSCRIBED" to NotificationKind.ACTIVITY_REPLY_SUBSCRIBED,
        "ACTIVITY_MENTION" to NotificationKind.ACTIVITY_MENTION,
        "ACTIVITY_MESSAGE" to NotificationKind.ACTIVITY_MESSAGE,
        "THREAD_COMMENT_REPLY" to NotificationKind.THREAD_COMMENT_REPLY,
        "THREAD_COMMENT_MENTION" to NotificationKind.THREAD_COMMENT_MENTION,
        "THREAD_SUBSCRIBED" to NotificationKind.THREAD_SUBSCRIBED,
        "THREAD_COMMENT_LIKE" to NotificationKind.THREAD_COMMENT_LIKE,
        "THREAD_LIKE" to NotificationKind.THREAD_LIKE,
        "RELATED_MEDIA_ADDITION" to NotificationKind.RELATED_MEDIA_ADDITION,
        "MEDIA_DATA_CHANGE" to NotificationKind.MEDIA_DATA_CHANGE,
        "MEDIA_MERGE" to NotificationKind.MEDIA_MERGE,
        "MEDIA_DELETION" to NotificationKind.MEDIA_DELETION,
        "MEDIA_SUBMISSION_UPDATE" to NotificationKind.MEDIA_SUBMISSION_UPDATE,
        "STAFF_SUBMISSION_UPDATE" to NotificationKind.STAFF_SUBMISSION_UPDATE,
        "CHARACTER_SUBMISSION_UPDATE" to NotificationKind.CHARACTER_SUBMISSION_UPDATE,
    )

    // ────────────── query shape ──────────────

    @Test
    fun queryAsksTheCountOnPageOneAndCarriesEveryFragment() = runBlocking {
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
            "MediaSubmissionUpdateNotification",
            "StaffSubmissionUpdateNotification",
            "CharacterSubmissionUpdateNotification",
            "ThreadCommentSubscribedNotification",
            "ThreadLikeNotification",
        ).forEach { assertTrue(first.query.contains("... on $it"), "missing fragment for $it") }
        assertTrue(first.query.contains("thread { id title }"), "forum fragments need the thread title")
        assertTrue(first.query.contains("deletedMediaTitles"), "MediaMerge needs the deleted titles")
        assertTrue(first.query.contains("submittedTitle"), "media submission needs the submitted title")

        assertEquals(false, withCount(client.requests[1]), "later pages do not ask the count")
    }

    // ────────────── mapper: kind ──────────────

    @Test
    fun mapsEveryTypeToItsKindAndNeverDropsAnUnknownOne() = runBlocking {
        val nodes = allKinds.mapIndexed { index, (type, _) -> node(index + 1, type) }
            .plus(node(99, "SOMETHING_NEW"))
        val client = FakeGraphQL(listOf(notificationPage(nodes)))

        val page = successValue(
            NotificationsRepositoryImpl(client).loadNotifications(1, false, NotificationFilter.ALL),
        )
        val byId = page.notifications.associateBy { it.id }

        allKinds.forEachIndexed { index, (type, kind) ->
            assertEquals(kind, byId.getValue(index + 1).kind, type)
        }
        assertEquals(NotificationKind.UNKNOWN, byId.getValue(99).kind, "an unknown type is kept")
    }

    // ────────────── mapper: targets ──────────────

    @Test
    fun mapsEachTargetFamilyAndDeletionHasNoDestination() = runBlocking {
        val client = FakeGraphQL(listOf(
            notificationPage(listOf(
                node(1, "ACTIVITY_LIKE", activityId = 5),
                node(2, "THREAD_LIKE", threadId = 9),
                node(3, "THREAD_COMMENT_REPLY", thread = 9, commentId = 3),
                node(4, "FOLLOWING", userName = "bob"),
                node(5, "MEDIA_DELETION"),
                node(6, "SOMETHING_NEW"),
                node(7, "MEDIA_DATA_CHANGE", mediaId = 11),
                node(8, "STAFF_SUBMISSION_UPDATE", staffName = "Anno"),
                node(9, "CHARACTER_SUBMISSION_UPDATE", characterName = "Luffy"),
                node(10, "MEDIA_SUBMISSION_UPDATE", submittedTitle = "New", mediaId = 12),
                node(11, "MEDIA_SUBMISSION_UPDATE", submittedTitle = "Rejected"),
            )),
        ))
        val page = successValue(
            NotificationsRepositoryImpl(client).loadNotifications(1, false, NotificationFilter.ALL),
        )
        val byId = page.notifications.associateBy { it.id }

        assertEquals("https://anilist.co/activity/5", webUrl(byId.getValue(1).target))
        assertEquals("https://anilist.co/forum/thread/9", webUrl(byId.getValue(2).target))
        assertEquals("https://anilist.co/forum/thread/9/comment/3", webUrl(byId.getValue(3).target))
        assertEquals("https://anilist.co/user/bob", webUrl(byId.getValue(4).target))
        assertEquals(NotificationTarget.None, byId.getValue(5).target)
        assertEquals("https://anilist.co/notifications", webUrl(byId.getValue(6).target))
        assertEquals(NotificationTarget.Media(11), byId.getValue(7).target)
        assertEquals(NotificationTarget.Staff(8), byId.getValue(8).target)
        assertEquals(NotificationTarget.Character(9), byId.getValue(9).target)
        assertEquals(NotificationTarget.Media(12), byId.getValue(10).target)
        assertEquals(NotificationTarget.None, byId.getValue(11).target, "a rejected submission has no media")
    }

    @Test
    fun carriesTheThreadTitleMergedTitlesAndSubmissionFields() = runBlocking {
        val client = FakeGraphQL(listOf(
            notificationPage(listOf(
                node(1, "THREAD_LIKE", threadId = 9, threadTitle = "Best of 2026", userName = "bob"),
                node(2, "MEDIA_MERGE", mediaId = 4, deletedMediaTitles = listOf("A", "B")),
                node(3, "MEDIA_SUBMISSION_UPDATE", submittedTitle = "My show", status = "PENDING", notes = "n"),
                node(4, "STAFF_SUBMISSION_UPDATE", staffName = "Anno", status = "REJECTED"),
                node(5, "CHARACTER_SUBMISSION_UPDATE", characterName = "Luffy", status = "ACCEPTED"),
            )),
        ))
        val page = successValue(
            NotificationsRepositoryImpl(client).loadNotifications(1, false, NotificationFilter.ALL),
        )
        val byId = page.notifications.associateBy { it.id }

        assertEquals("Best of 2026", byId.getValue(1).threadTitle)
        assertEquals(listOf("A", "B"), byId.getValue(2).deletedMediaTitles)
        assertEquals("My show", byId.getValue(3).subjectName)
        assertEquals("PENDING", byId.getValue(3).status)
        assertEquals("n", byId.getValue(3).notes)
        assertEquals("Anno", byId.getValue(4).subjectName)
        assertEquals("Luffy", byId.getValue(5).subjectName)
    }

    @Test
    fun blanksAreNormalizedToNullSoTheFallbackIsUsed() = runBlocking {
        val client = FakeGraphQL(listOf(
            notificationPage(listOf(
                node(1, "FOLLOWING", userName = ""),
                node(2, "THREAD_LIKE", threadId = 9, threadTitle = "   "),
                node(3, "MEDIA_MERGE", mediaId = 4, deletedMediaTitles = listOf("", "A", "  ")),
                node(4, "MEDIA_SUBMISSION_UPDATE", submittedTitle = "", mediaId = 12),
            )),
        ))
        val page = successValue(
            NotificationsRepositoryImpl(client).loadNotifications(1, false, NotificationFilter.ALL),
        )
        val byId = page.notifications.associateBy { it.id }

        assertNull(byId.getValue(1).userName, "a blank actor falls back")
        assertNull(byId.getValue(2).threadTitle, "a blank thread title falls back")
        assertEquals(listOf("A"), byId.getValue(3).deletedMediaTitles, "blank entries are dropped")
        assertEquals(
            "Media 12",
            byId.getValue(4).subjectName,
            "a blank submitted title falls back to the media title",
        )
    }

    // ────────────── filters ──────────────

    @Test
    fun filtersCarryTheExactOverlappingKindsWithoutUnknown() {
        assertEquals(null, NotificationFilter.ALL.kinds())
        assertEquals(listOf(NotificationKind.AIRING), NotificationFilter.AIRING.kinds())
        assertEquals(listOf(NotificationKind.FOLLOWING), NotificationFilter.FOLLOWS.kinds())
        assertEquals(
            listOf(
                NotificationKind.ACTIVITY_LIKE,
                NotificationKind.ACTIVITY_REPLY,
                NotificationKind.ACTIVITY_REPLY_LIKE,
                NotificationKind.ACTIVITY_MENTION,
                NotificationKind.ACTIVITY_REPLY_SUBSCRIBED,
                NotificationKind.ACTIVITY_MESSAGE,
            ),
            NotificationFilter.ACTIVITY.kinds(),
        )
        assertEquals(
            listOf(
                NotificationKind.ACTIVITY_MESSAGE,
                NotificationKind.ACTIVITY_REPLY,
                NotificationKind.ACTIVITY_REPLY_SUBSCRIBED,
                NotificationKind.ACTIVITY_MENTION,
                NotificationKind.THREAD_COMMENT_REPLY,
                NotificationKind.THREAD_COMMENT_MENTION,
                NotificationKind.THREAD_SUBSCRIBED,
            ),
            NotificationFilter.REPLIES.kinds(),
        )
        assertEquals(
            listOf(
                NotificationKind.THREAD_COMMENT_REPLY,
                NotificationKind.THREAD_COMMENT_MENTION,
                NotificationKind.THREAD_SUBSCRIBED,
                NotificationKind.THREAD_COMMENT_LIKE,
                NotificationKind.THREAD_LIKE,
            ),
            NotificationFilter.FORUM.kinds(),
        )
        assertEquals(
            listOf(
                NotificationKind.RELATED_MEDIA_ADDITION,
                NotificationKind.MEDIA_DATA_CHANGE,
                NotificationKind.MEDIA_MERGE,
                NotificationKind.MEDIA_DELETION,
                NotificationKind.MEDIA_SUBMISSION_UPDATE,
                NotificationKind.STAFF_SUBMISSION_UPDATE,
                NotificationKind.CHARACTER_SUBMISSION_UPDATE,
            ),
            NotificationFilter.MEDIA.kinds(),
        )

        NotificationFilter.entries.forEach { filter ->
            assertFalse(
                filter.kinds()?.contains(NotificationKind.UNKNOWN) == true,
                "$filter must never ask for UNKNOWN",
            )
            assertTrue(
                filter.toAniListTypes()?.all { it.isNotBlank() } != false,
                "$filter must only send real API types",
            )
        }
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

    // ────────────── holder: open / refresh ──────────────

    @Test
    fun openingResetsTheCountAndReopeningWithinTheTtlDoesNotRequest() {
        val client = FakeGraphQL(listOf(notificationPage(listOf(node(1, "AIRING")), unreadCount = 5)))
        val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), unconfined())

        holder.onOpen()

        assertEquals(1, client.calls)
        assertEquals(true, resetFlag(client.requests[0]), "the first open marks the feed read")
        assertEquals(0, holder.unreadCount, "the dot disappears")

        holder.onOpen()

        assertEquals(1, client.calls, "a fresh, already-read feed is not re-asked")
    }

    @Test
    fun anUnreadCountForcesAResetOnReopen() {
        val client = FakeGraphQL(listOf(
            notificationPage(listOf(node(1, "AIRING")), unreadCount = 5),
            notificationPage(listOf(node(1, "AIRING")), unreadCount = 5),
        ))
        val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), unconfined())

        holder.onOpen()
        assertEquals(1, client.calls)
        assertEquals(0, holder.unreadCount)

        holder.updateUnreadCount(3) // the profile reports new unread
        holder.onOpen()

        assertEquals(2, client.calls, "unread is a reason to ask again")
        assertEquals(true, resetFlag(client.requests[1]))
        assertEquals(0, holder.unreadCount)
    }

    @Test
    fun openingWithAPreviousFilterReturnsToAllAndLoadsEvenWhenFresh() {
        val client = FakeGraphQL(listOf(
            notificationPage(listOf(node(1, "AIRING")), unreadCount = 5),
            notificationPage(listOf(node(9, "THREAD_LIKE"))),
            notificationPage(listOf(node(1, "AIRING")), unreadCount = 0),
        ))
        val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), unconfined())

        holder.onOpen() // ALL, reset: fresh + read afterwards
        assertEquals(1, client.calls)
        holder.selectFilter(NotificationFilter.FORUM)
        assertEquals(2, client.calls)
        assertEquals(false, resetFlag(client.requests[1]), "a filter change does not reset")

        holder.onOpen() // back to ALL, the list was cleared → must load even if fresh

        assertEquals(3, client.calls, "a cleared list must not be left empty")
        assertEquals(NotificationFilter.ALL, holder.selectedFilter)
        assertEquals(true, resetFlag(client.requests[2]))
        assertEquals(listOf(1), idsOf(holder))
    }

    @Test
    fun refreshResetsOnlyOnTheAllFilter() {
        val client = FakeGraphQL(listOf(
            notificationPage(listOf(node(5, "THREAD_LIKE"))),
            notificationPage(listOf(node(5, "THREAD_LIKE"))),
            notificationPage(listOf(node(1, "AIRING"))),
            notificationPage(listOf(node(1, "AIRING"))),
        ))
        val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), unconfined())

        holder.selectFilter(NotificationFilter.FORUM)
        holder.refresh()
        assertEquals(2, client.calls)
        assertEquals(false, resetFlag(client.requests[1]), "a filter refresh does not reset")

        holder.selectFilter(NotificationFilter.ALL)
        holder.refresh()
        assertEquals(4, client.calls)
        assertEquals(true, resetFlag(client.requests[3]), "the ALL refresh resets the count")
    }

    // ────────────── holder: pagination ──────────────

    @Test
    fun loadMoreAppendsTheNextPage() {
        val client = FakeGraphQL(listOf(
            notificationPage(listOf(node(1, "AIRING")), hasNextPage = true),
            notificationPage(listOf(node(2, "AIRING")), hasNextPage = false),
        ))
        val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), unconfined())

        holder.load()
        holder.loadMore()

        assertEquals(listOf(1, 2), idsOf(holder))
        assertFalse(holder.hasNextPage)
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
            holder.refresh()
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

    private fun <T> successValue(result: NetworkResult<T>): T = when (result) {
        is NetworkResult.Success -> result.value
        is NetworkResult.Failure -> throw AssertionError("expected Success but was $result")
    }

    @Test
    fun aFailedLoadMoreGoesToActionErrorNotTheBlockingError() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeGraphQL(
                listOf(notificationPage(listOf(node(1, "AIRING")), hasNextPage = true)),
                failureAt = 1,
            )
            val holder = NotificationsStateHolder(NotificationsRepositoryImpl(client), scope)

            holder.load()
            dispatcher.runAll()
            assertEquals(listOf(1), idsOf(holder))
            assertTrue(holder.hasNextPage)

            holder.loadMore() // page 2 fails
            dispatcher.runAll()

            assertNull(holder.error, "a failed page must not blank the loaded feed")
            assertTrue(holder.actionError != null)

            holder.clearActionError()
            assertNull(holder.actionError)
        } finally {
            scope.cancel()
        }
    }
}
