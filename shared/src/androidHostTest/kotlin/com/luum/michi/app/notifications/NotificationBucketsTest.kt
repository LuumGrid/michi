package com.luum.michi.app.notifications

import com.luum.michi.app.core.domain.model.NotificationBucket
import com.luum.michi.app.core.domain.model.apiTypes
import com.luum.michi.app.core.domain.model.notificationBucketFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tripwire: the full known AniList NotificationType universe, both directions.
 *  If AniList ships an 18th type, this fails loudly instead of drifting silently. */
private val KnownTypes = mapOf(
    "AIRING" to NotificationBucket.AIRING,
    "FOLLOWING" to NotificationBucket.FOLLOWING,
    "ACTIVITY_LIKE" to NotificationBucket.ACTIVITY,
    "ACTIVITY_REPLY" to NotificationBucket.ACTIVITY,
    "ACTIVITY_REPLY_LIKE" to NotificationBucket.ACTIVITY,
    "ACTIVITY_MENTION" to NotificationBucket.ACTIVITY,
    "ACTIVITY_REPLY_SUBSCRIBED" to NotificationBucket.ACTIVITY,
    "ACTIVITY_MESSAGE" to NotificationBucket.MESSAGE,
    "THREAD_COMMENT_REPLY" to NotificationBucket.FORUM,
    "THREAD_COMMENT_MENTION" to NotificationBucket.FORUM,
    "THREAD_SUBSCRIBED" to NotificationBucket.FORUM,
    "THREAD_COMMENT_LIKE" to NotificationBucket.FORUM,
    "THREAD_LIKE" to NotificationBucket.FORUM,
    "RELATED_MEDIA_ADDITION" to NotificationBucket.MEDIA,
    "MEDIA_DATA_CHANGE" to NotificationBucket.MEDIA,
    "MEDIA_MERGE" to NotificationBucket.MEDIA,
    "MEDIA_DELETION" to NotificationBucket.MEDIA,
)

class NotificationBucketsTest {

    @Test
    fun everyKnownTypeResolvesToItsBucket() {
        KnownTypes.forEach { (type, bucket) ->
            assertEquals(bucket, notificationBucketFor(type), type)
        }
    }

    @Test
    fun bucketsContainNoUnknownTypes() {
        NotificationBucket.entries.flatMap { it.apiTypes }.forEach { type ->
            assertTrue(type in KnownTypes, type)
        }
    }

    @Test
    fun unknownTypeResolvesToNull() {
        assertNull(notificationBucketFor("BOGUS"))
    }
}
