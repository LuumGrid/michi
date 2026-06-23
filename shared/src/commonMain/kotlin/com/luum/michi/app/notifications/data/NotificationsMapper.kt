package com.luum.michi.app.notifications.data

import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.anilist.dto.NotificationNodeDto
import com.luum.michi.app.notifications.presentation.model.AppNotification
import com.luum.michi.app.notifications.presentation.model.NotificationCategory
import com.luum.michi.app.notifications.presentation.model.NotificationFilter
import com.luum.michi.app.notifications.presentation.model.NotificationTarget

private fun MediaTitleDto?.bestTitle(): String? {
    if (this == null) return null
    return userPreferred ?: english ?: romaji ?: native
}

private fun categoryFor(type: String): NotificationCategory = when (type) {
    "AIRING" -> NotificationCategory.AIRING
    "FOLLOWING" -> NotificationCategory.FOLLOWING
    "ACTIVITY_LIKE",
    "ACTIVITY_REPLY",
    "ACTIVITY_REPLY_LIKE",
    "ACTIVITY_MENTION" -> NotificationCategory.ACTIVITY
    "ACTIVITY_MESSAGE" -> NotificationCategory.MESSAGE
    "RELATED_MEDIA_ADDITION",
    "MEDIA_DATA_CHANGE",
    "MEDIA_MERGE",
    "MEDIA_DELETION" -> NotificationCategory.MEDIA_CHANGE
    "THREAD_COMMENT_REPLY",
    "THREAD_COMMENT_MENTION" -> NotificationCategory.FORUM
    else -> NotificationCategory.OTHER
}

internal fun NotificationNodeDto.toAppNotification(): AppNotification? {
    val notificationId = id ?: return null
    val notificationType = type ?: return null
    val category = categoryFor(notificationType)

    val imageUrl = media?.coverImage?.let { it.extraLarge ?: it.large ?: it.medium }
        ?: user?.avatar?.let { it.large ?: it.medium }

    val target: NotificationTarget = when {
        media?.id != null -> NotificationTarget.Media(media.id)
        category == NotificationCategory.ACTIVITY ||
            category == NotificationCategory.FORUM ||
            category == NotificationCategory.MESSAGE ||
            category == NotificationCategory.FOLLOWING -> NotificationTarget.Web("https://anilist.co/notifications")
        else -> NotificationTarget.None
    }

    return AppNotification(
        id = notificationId,
        category = category,
        imageUrl = imageUrl,
        createdAtEpochSeconds = createdAt ?: 0L,
        target = target,
        episode = episode,
        mediaTitle = media?.title.bestTitle() ?: deletedMediaTitle,
        userName = user?.name,
    )
}

internal fun NotificationFilter.toAniListTypes(): List<String>? = when (this) {
    NotificationFilter.ALL -> null
    NotificationFilter.AIRING -> listOf("AIRING")
    NotificationFilter.ACTIVITY -> listOf(
        "ACTIVITY_MESSAGE", "ACTIVITY_REPLY", "ACTIVITY_MENTION",
        "ACTIVITY_LIKE", "ACTIVITY_REPLY_LIKE", "ACTIVITY_REPLY_SUBSCRIBED",
    )
    NotificationFilter.FORUM -> listOf(
        "THREAD_COMMENT_REPLY", "THREAD_COMMENT_MENTION", "THREAD_SUBSCRIBED",
        "THREAD_COMMENT_LIKE", "THREAD_LIKE",
    )
    NotificationFilter.FOLLOWS -> listOf("FOLLOWING")
    NotificationFilter.MEDIA -> listOf(
        "RELATED_MEDIA_ADDITION", "MEDIA_DATA_CHANGE", "MEDIA_MERGE", "MEDIA_DELETION",
    )
}
