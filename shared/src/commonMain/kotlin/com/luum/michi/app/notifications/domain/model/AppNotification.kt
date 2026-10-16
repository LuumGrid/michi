package com.luum.michi.app.notifications.domain.model
import com.luum.michi.app.core.model.CoverSizes

internal enum class NotificationCategory { AIRING, FOLLOWING, ACTIVITY, FORUM, MESSAGE, MEDIA_CHANGE, REPLIES, OTHER }

internal sealed interface NotificationTarget {
    data class Media(val id: Int) : NotificationTarget
    data class Web(val url: String) : NotificationTarget
}

internal data class AppNotification(
    val id: Int,
    val category: NotificationCategory,
    val image: CoverSizes?,
    val createdAtEpochSeconds: Long,
    val target: NotificationTarget,
    val episode: Int? = null,
    val mediaTitle: String? = null,
    val userName: String? = null,
)
