package com.luum.michi.app.notifications.domain.model

internal enum class NotificationCategory { AIRING, FOLLOWING, ACTIVITY, FORUM, MESSAGE, MEDIA_CHANGE, REPLIES, OTHER }

internal sealed interface NotificationTarget {
    data class Media(val id: Int) : NotificationTarget
    data class Web(val url: String) : NotificationTarget
    data object None : NotificationTarget
}

internal data class AppNotification(
    val id: Int,
    val category: NotificationCategory,
    val imageUrl: String?,
    val createdAtEpochSeconds: Long,
    val target: NotificationTarget,
    val episode: Int? = null,
    val mediaTitle: String? = null,
    val userName: String? = null,
)
