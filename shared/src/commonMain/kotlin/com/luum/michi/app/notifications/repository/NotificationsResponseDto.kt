package com.luum.michi.app.notifications.repository

import com.luum.michi.app.core.media.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.media.repository.dto.MediaTitleDto
import com.luum.michi.app.core.network.repository.dto.PageInfoDto
import com.luum.michi.app.core.person.repository.dto.PersonImageDto
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
    /** Forum: the replied/liked comment id (comment-based notifications). */
    val commentId: Int? = null,
    /** Forum: the liked thread id (`ThreadLikeNotification`). */
    val threadId: Int? = null,
    val thread: NotificationThreadDto? = null,
    val deletedMediaTitle: String? = null,
    val media: NotificationMediaDto? = null,
    val user: NotificationUserDto? = null,
)

@Serializable
internal data class NotificationThreadDto(
    val id: Int? = null,
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
