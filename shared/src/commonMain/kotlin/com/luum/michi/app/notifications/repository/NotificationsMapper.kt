package com.luum.michi.app.notifications.repository

import com.luum.michi.app.core.media.repository.bestTitleOrNull
import com.luum.michi.app.core.model.NotificationBucket
import com.luum.michi.app.core.model.apiTypes
import com.luum.michi.app.core.model.notificationBucketFor
import com.luum.michi.app.notifications.domain.model.AppNotification
import com.luum.michi.app.notifications.domain.model.NotificationCategory
import com.luum.michi.app.notifications.domain.model.NotificationFilter
import com.luum.michi.app.notifications.domain.model.NotificationTarget
import com.luum.michi.app.core.media.repository.toCoverSizes
import com.luum.michi.app.core.model.CoverSizes

/** Reply types, shown under their own category and filter. Single definition:
 *  both categoryFor() and toAniListTypes() derive from here so they cannot drift. */
private val ReplyTypes = listOf("ACTIVITY_REPLY", "ACTIVITY_REPLY_LIKE", "THREAD_COMMENT_REPLY")

private fun categoryFor(type: String): NotificationCategory = when {
    type in ReplyTypes -> NotificationCategory.REPLIES
    else -> when (notificationBucketFor(type)) {
        NotificationBucket.AIRING -> NotificationCategory.AIRING
        NotificationBucket.FOLLOWING -> NotificationCategory.FOLLOWING
        NotificationBucket.ACTIVITY -> NotificationCategory.ACTIVITY
        NotificationBucket.MESSAGE -> NotificationCategory.MESSAGE
        NotificationBucket.FORUM -> NotificationCategory.FORUM
        NotificationBucket.MEDIA -> NotificationCategory.MEDIA_CHANGE
        null -> NotificationCategory.OTHER
    }
}

internal fun NotificationNodeDto.toAppNotification(): AppNotification? {
    val notificationId = id ?: return null
    val notificationType = type ?: return null
    val category = categoryFor(notificationType)

    val image = media?.coverImage?.toCoverSizes()
        ?: user?.avatar?.let { CoverSizes(medium = it.medium, large = it.large) }

    val target: NotificationTarget = when {
        media?.id != null -> NotificationTarget.Media(media.id)
        else -> webTarget(category)
    }

    return AppNotification(
        id = notificationId,
        category = category,
        image = image,
        createdAtEpochSeconds = createdAt ?: 0L,
        target = target,
        episode = episode,
        mediaTitle = media?.title.bestTitleOrNull() ?: deletedMediaTitle,
        userName = user?.name,
    )
}

/** Fallback destination: any notification without a usable id still opens the feed. */
private const val AniListNotificationsUrl = "https://anilist.co/notifications"

/**
 * The web destination of a non-media notification, decided by the data it
 * carries (all URLs are built here, the single place):
 *  - an activity, reply, reply-like, mention, reply-subscribed or message has
 *    `activityId` → that activity;
 *  - a forum reply/like/subscribed has `thread { id }` (+ `commentId`) or a
 *    thread like has `threadId` → the thread, with `/comment/{id}` when known;
 *  - a follow has no id, only the actor → the user profile.
 * Anything else (e.g. a deleted media with no id) falls back to the feed.
 */
private fun NotificationNodeDto.webTarget(category: NotificationCategory): NotificationTarget {
    activityId?.let { return NotificationTarget.Web("https://anilist.co/activity/$it") }
    val forumThreadId = thread?.id ?: threadId
    if (forumThreadId != null) {
        val forumCommentId = commentId
        val commentPath = if (forumCommentId != null && forumCommentId > 0) "/comment/$forumCommentId" else ""
        return NotificationTarget.Web("https://anilist.co/forum/thread/$forumThreadId$commentPath")
    }
    val name = user?.name
    if (category == NotificationCategory.FOLLOWING && !name.isNullOrBlank()) {
        return NotificationTarget.Web("https://anilist.co/user/$name")
    }
    return NotificationTarget.Web(AniListNotificationsUrl)
}

internal fun NotificationFilter.toAniListTypes(): List<String>? = when (this) {
    NotificationFilter.ALL -> null
    NotificationFilter.AIRING -> NotificationBucket.AIRING.apiTypes
    // The activity filter historically includes direct messages as well; preserved.
    // Reply types live under REPLIES now, so they are excluded here (and
    // THREAD_COMMENT_REPLY from FORUM below) to keep filters exclusive.
    NotificationFilter.ACTIVITY -> NotificationBucket.ACTIVITY.apiTypes +
        NotificationBucket.MESSAGE.apiTypes -
        ReplyTypes.toSet()
    NotificationFilter.REPLIES -> ReplyTypes
    NotificationFilter.FORUM -> NotificationBucket.FORUM.apiTypes - ReplyTypes.toSet()
    NotificationFilter.FOLLOWS -> NotificationBucket.FOLLOWING.apiTypes
    NotificationFilter.MEDIA -> NotificationBucket.MEDIA.apiTypes
}
