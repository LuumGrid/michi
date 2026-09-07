package com.luum.michi.app.core.domain.model

/**
 * Neutral buckets grouping the AniList `NotificationType` values.
 *
 * Single source of truth for which API type belongs to which bucket. Both the
 * notifications feature (display category + server-side filter) and the settings
 * feature (notification preference buckets) derive from here, so the three lists
 * cannot drift apart again: adding a type means adding it to exactly one bucket.
 */
internal enum class NotificationBucket { AIRING, ACTIVITY, FOLLOWING, FORUM, MESSAGE, MEDIA }

internal val NotificationBucket.apiTypes: List<String>
    get() = when (this) {
        NotificationBucket.AIRING -> listOf("AIRING")
        NotificationBucket.ACTIVITY -> listOf(
            "ACTIVITY_LIKE",
            "ACTIVITY_REPLY",
            "ACTIVITY_REPLY_LIKE",
            "ACTIVITY_MENTION",
            "ACTIVITY_REPLY_SUBSCRIBED",
        )
        NotificationBucket.FOLLOWING -> listOf("FOLLOWING")
        NotificationBucket.FORUM -> listOf(
            "THREAD_COMMENT_REPLY",
            "THREAD_COMMENT_MENTION",
            "THREAD_SUBSCRIBED",
            "THREAD_COMMENT_LIKE",
            "THREAD_LIKE",
        )
        NotificationBucket.MESSAGE -> listOf("ACTIVITY_MESSAGE")
        NotificationBucket.MEDIA -> listOf(
            "RELATED_MEDIA_ADDITION",
            "MEDIA_DATA_CHANGE",
            "MEDIA_MERGE",
            "MEDIA_DELETION",
        )
    }

/** Reverse lookup: which bucket an AniList notification type belongs to, or null if unknown. */
internal fun notificationBucketFor(apiType: String): NotificationBucket? =
    NotificationBucket.entries.firstOrNull { apiType in it.apiTypes }
