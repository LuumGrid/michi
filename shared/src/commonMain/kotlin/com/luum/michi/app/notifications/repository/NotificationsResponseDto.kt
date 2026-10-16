package com.luum.michi.app.notifications.repository

import com.luum.michi.app.core.network.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.network.repository.dto.MediaTitleDto
import com.luum.michi.app.core.network.repository.dto.PageInfoDto
import com.luum.michi.app.core.network.repository.dto.PersonImageDto
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
    val pageInfo: PageInfoDto = PageInfoDto(),
    val notifications: List<NotificationNodeDto?> = emptyList(),
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
    val avatar: PersonImageDto? = null,
)
