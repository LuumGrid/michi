package com.luum.michi.app.notifications.ui.state

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.notifications.domain.NotificationsRepository
import com.luum.michi.app.notifications.domain.model.AppNotification
import com.luum.michi.app.notifications.domain.model.NotificationFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class NotificationsStateHolder(
    private val repository: NotificationsRepository,
    private val scope: CoroutineScope,
) {
    private val backing = mutableListOf<AppNotification>()
    val notifications: List<AppNotification> get() = backing

    var isLoading = false
        private set
    var isLoadingMore = false
        private set
    var hasNextPage = false
        private set
    var error: NetworkError? = null
        private set
    var unreadCount = 0
        private set
    var selectedFilter = NotificationFilter.ALL
        private set

    private var currentPage = 1
    private var currentJob: Job? = null
    private var loadMoreJob: Job? = null

    fun load(resetCount: Boolean = false) {
        currentJob?.cancel()
        loadMoreJob?.cancel()
        backing.clear()
        currentPage = 1
        hasNextPage = false
        error = null
        isLoading = true
        currentJob = scope.launch {
            try {
                when (val result = repository.loadNotifications(page = 1, resetCount = resetCount, filter = selectedFilter)) {
                    is NetworkResult.Success -> {
                        val page = result.value
                        backing.addAll(page.notifications)
                        hasNextPage = page.hasNextPage
                        currentPage = 1
                        page.unreadCount?.let { unreadCount = it }
                        if (resetCount) unreadCount = 0
                        error = null
                    }
                    is NetworkResult.Failure -> {
                        error = result.error
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasNextPage) return
        val nextPage = currentPage + 1
        isLoadingMore = true
        loadMoreJob = scope.launch {
            when (val result = repository.loadNotifications(page = nextPage, resetCount = false, filter = selectedFilter)) {
                is NetworkResult.Success -> {
                    val page = result.value
                    backing.addAll(page.notifications)
                    hasNextPage = page.hasNextPage
                    currentPage = nextPage
                }
                is NetworkResult.Failure -> {
                    error = result.error
                }
            }
            isLoadingMore = false
        }
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createNotificationsStateHolder(
    repository: NotificationsRepository,
    scope: CoroutineScope,
): NotificationsStateHolder {
    return NotificationsStateHolder(repository, scope)
}
