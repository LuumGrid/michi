package com.luum.michi.app.core.model

/**
 * Neutral buckets grouping the AniList `NotificationType` values.
 *
 * Single source of truth for the settings feature's notification preference
 * buckets. The notifications feature keeps its own exact-type vocabulary
 * (`NotificationKind`), so filters can overlap (like Otraku) without changing
 * what settings writes to AniList.
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
