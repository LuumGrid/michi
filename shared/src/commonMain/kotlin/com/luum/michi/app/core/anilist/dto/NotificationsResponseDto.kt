package com.luum.michi.app.core.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NotificationsResponseDto(
    @SerialName("Page") val page: NotificationsPageDto = NotificationsPageDto(),
    @SerialName("Viewer") val viewer: NotificationsViewerDto? = null,
)

@Serializable
internal data class NotificationsViewerDto(
    val unreadNotificationCount: Int? = null,
)

@Serializable
internal data class NotificationsPageDto(
    val pageInfo: NotificationsPageInfoDto = NotificationsPageInfoDto(),
    val notifications: List<NotificationNodeDto?> = emptyList(),
)

@Serializable
internal data class NotificationsPageInfoDto(
    val hasNextPage: Boolean = false,
)

@Serializable
internal data class NotificationNodeDto(
    val id: Int? = null,
    val type: String? = null,
    val createdAt: Long? = null,
    val episode: Int? = null,
    val reason: String? = null,
    val activityId: Int? = null,
    val deletedMediaTitle: String? = null,
    val media: NotificationMediaDto? = null,
    val user: NotificationUserDto? = null,
)

@Serializable
internal data class NotificationMediaDto(
    val id: Int? = null,
    val title: MediaTitleDto? = null,
    val coverImage: MediaCoverImageDto? = null,
)

@Serializable
internal data class NotificationUserDto(
    val id: Int? = null,
    val name: String? = null,
    val avatar: NotificationAvatarDto? = null,
)

@Serializable
internal data class NotificationAvatarDto(
    val large: String? = null,
    val medium: String? = null,
) {
    val bestUrl: String? get() = large ?: medium
}
