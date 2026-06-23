package com.luum.michi.app.notifications.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.notifications.presentation.model.AppNotification
import com.luum.michi.app.notifications.presentation.model.NotificationFilter

internal data class NotificationsPage(
    val notifications: List<AppNotification>,
    val hasNextPage: Boolean,
    val unreadCount: Int?,
)

internal interface NotificationsRepository {
    suspend fun loadNotifications(page: Int, resetCount: Boolean, filter: NotificationFilter): NetworkResult<NotificationsPage>
}
