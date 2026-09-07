package com.luum.michi.app.notifications.repository

import com.luum.michi.app.core.repository.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.repository.anilist.dto.NotificationNodeDto
import com.luum.michi.app.core.domain.model.NotificationBucket
import com.luum.michi.app.core.domain.model.apiTypes
import com.luum.michi.app.core.domain.model.notificationBucketFor
import com.luum.michi.app.notifications.domain.model.AppNotification
import com.luum.michi.app.notifications.domain.model.NotificationCategory
import com.luum.michi.app.notifications.domain.model.NotificationFilter
import com.luum.michi.app.notifications.domain.model.NotificationTarget

private fun MediaTitleDto?.bestTitle(): String? {
    if (this == null) return null
    return userPreferred ?: english ?: romaji ?: native
}

private fun categoryFor(type: String): NotificationCategory = when (notificationBucketFor(type)) {
    NotificationBucket.AIRING -> NotificationCategory.AIRING
    NotificationBucket.FOLLOWING -> NotificationCategory.FOLLOWING
    NotificationBucket.ACTIVITY -> NotificationCategory.ACTIVITY
    NotificationBucket.MESSAGE -> NotificationCategory.MESSAGE
    NotificationBucket.FORUM -> NotificationCategory.FORUM
    NotificationBucket.MEDIA -> NotificationCategory.MEDIA_CHANGE
    null -> NotificationCategory.OTHER
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
    NotificationFilter.AIRING -> NotificationBucket.AIRING.apiTypes
    // The activity filter historically includes direct messages as well; preserved.
    NotificationFilter.ACTIVITY -> NotificationBucket.ACTIVITY.apiTypes + NotificationBucket.MESSAGE.apiTypes
    NotificationFilter.FORUM -> NotificationBucket.FORUM.apiTypes
    NotificationFilter.FOLLOWS -> NotificationBucket.FOLLOWING.apiTypes
    NotificationFilter.MEDIA -> NotificationBucket.MEDIA.apiTypes
}
