package com.luum.michi.app.notifications.domain.model

import com.luum.michi.app.core.language.domain.LanguageStrings

internal enum class NotificationFilter { ALL, AIRING, ACTIVITY, REPLIES, FORUM, FOLLOWS, MEDIA }

/**
 * The exact kinds each filter asks AniList for, or null for ALL (no `type_in`).
 * Deliberately overlapping (like Otraku): the activity filter keeps messages and
 * replies keep the activity ones they came from. `UNKNOWN` is never listed.
 */
internal fun NotificationFilter.kinds(): List<NotificationKind>? = when (this) {
    NotificationFilter.ALL -> null
    NotificationFilter.AIRING -> listOf(NotificationKind.AIRING)
    NotificationFilter.ACTIVITY -> listOf(
        NotificationKind.ACTIVITY_LIKE,
        NotificationKind.ACTIVITY_REPLY,
        NotificationKind.ACTIVITY_REPLY_LIKE,
        NotificationKind.ACTIVITY_MENTION,
        NotificationKind.ACTIVITY_REPLY_SUBSCRIBED,
        NotificationKind.ACTIVITY_MESSAGE,
    )
    NotificationFilter.REPLIES -> listOf(
        NotificationKind.ACTIVITY_MESSAGE,
        NotificationKind.ACTIVITY_REPLY,
        NotificationKind.ACTIVITY_REPLY_SUBSCRIBED,
        NotificationKind.ACTIVITY_MENTION,
        NotificationKind.THREAD_COMMENT_REPLY,
        NotificationKind.THREAD_COMMENT_MENTION,
        NotificationKind.THREAD_SUBSCRIBED,
    )
    NotificationFilter.FORUM -> listOf(
        NotificationKind.THREAD_COMMENT_REPLY,
        NotificationKind.THREAD_COMMENT_MENTION,
        NotificationKind.THREAD_SUBSCRIBED,
        NotificationKind.THREAD_COMMENT_LIKE,
        NotificationKind.THREAD_LIKE,
    )
    NotificationFilter.FOLLOWS -> listOf(NotificationKind.FOLLOWING)
    NotificationFilter.MEDIA -> listOf(
        NotificationKind.RELATED_MEDIA_ADDITION,
        NotificationKind.MEDIA_DATA_CHANGE,
        NotificationKind.MEDIA_MERGE,
        NotificationKind.MEDIA_DELETION,
        NotificationKind.MEDIA_SUBMISSION_UPDATE,
        NotificationKind.STAFF_SUBMISSION_UPDATE,
        NotificationKind.CHARACTER_SUBMISSION_UPDATE,
    )
}

internal fun NotificationFilter.label(strings: LanguageStrings): String = when (this) {
    NotificationFilter.ALL -> strings.notificationFilterAll
    NotificationFilter.AIRING -> strings.notificationFilterAiring
    NotificationFilter.ACTIVITY -> strings.activityLabel
    NotificationFilter.REPLIES -> strings.notificationFilterReplies
    NotificationFilter.FORUM -> strings.forumLabel
    NotificationFilter.FOLLOWS -> strings.notificationFilterFollows
    NotificationFilter.MEDIA -> strings.notificationFilterMedia
}
