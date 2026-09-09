package com.luum.michi.app.notifications

import com.luum.michi.app.core.network.repository.dto.NotificationNodeDto
import com.luum.michi.app.notifications.domain.model.NotificationCategory
import com.luum.michi.app.notifications.domain.model.NotificationFilter
import com.luum.michi.app.notifications.repository.toAniListTypes
import com.luum.michi.app.notifications.repository.toAppNotification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun categoryOf(type: String): NotificationCategory? =
    NotificationNodeDto(id = 1, type = type).toAppNotification()?.category

class NotificationsRepliesTest {

    @Test
    fun repliesFilterRequestsExactlyTheReplyTypes() {
        assertEquals(
            listOf("ACTIVITY_REPLY", "ACTIVITY_REPLY_LIKE", "THREAD_COMMENT_REPLY"),
            NotificationFilter.REPLIES.toAniListTypes(),
        )
    }

    @Test
    fun activityFilterExcludesReplyTypes() {
        val types = NotificationFilter.ACTIVITY.toAniListTypes().orEmpty()
        assertFalse(types.contains("ACTIVITY_REPLY"))
        assertFalse(types.contains("ACTIVITY_REPLY_LIKE"))
        assertFalse(types.contains("THREAD_COMMENT_REPLY"))
        // Historical quirk preserved: direct messages still ride along.
        assertTrue(types.contains("ACTIVITY_MESSAGE"))
    }

    @Test
    fun forumFilterExcludesThreadCommentReply() {
        val types = NotificationFilter.FORUM.toAniListTypes().orEmpty()
        assertFalse(types.contains("THREAD_COMMENT_REPLY"))
        assertTrue(types.contains("THREAD_COMMENT_MENTION"))
        assertTrue(types.contains("THREAD_SUBSCRIBED"))
    }

    @Test
    fun replyTypesDisplayAsReplies() {
        assertEquals(NotificationCategory.REPLIES, categoryOf("ACTIVITY_REPLY"))
        assertEquals(NotificationCategory.REPLIES, categoryOf("ACTIVITY_REPLY_LIKE"))
        assertEquals(NotificationCategory.REPLIES, categoryOf("THREAD_COMMENT_REPLY"))
    }

    @Test
    fun neighborTypesKeepTheirDisplayCategory() {
        assertEquals(NotificationCategory.ACTIVITY, categoryOf("ACTIVITY_LIKE"))
        assertEquals(NotificationCategory.FORUM, categoryOf("THREAD_COMMENT_MENTION"))
        assertEquals(NotificationCategory.MESSAGE, categoryOf("ACTIVITY_MESSAGE"))
    }
}
