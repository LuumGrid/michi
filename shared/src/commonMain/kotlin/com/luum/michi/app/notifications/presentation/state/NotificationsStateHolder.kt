package com.luum.michi.app.notifications.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.notifications.data.NotificationsRepository
import com.luum.michi.app.notifications.presentation.model.AppNotification
import com.luum.michi.app.notifications.presentation.model.NotificationFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class NotificationsStateHolder(
    private val repository: NotificationsRepository,
    private val scope: CoroutineScope,
) {
    private val backing = mutableStateListOf<AppNotification>()
    val notifications: List<AppNotification> get() = backing

    var isLoading by mutableStateOf(false)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var hasNextPage by mutableStateOf(false)
        private set
    var error by mutableStateOf<NetworkError?>(null)
        private set
    var unreadCount by mutableStateOf(0)
        private set
    var selectedFilter by mutableStateOf(NotificationFilter.ALL)
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

    fun selectFilter(filter: NotificationFilter) {
        if (filter == selectedFilter) return
        selectedFilter = filter
        load(resetCount = false)
    }
}

@Composable
internal fun rememberNotificationsStateHolder(
    repository: NotificationsRepository,
): NotificationsStateHolder {
    val scope = rememberCoroutineScope()
    return remember {
        NotificationsStateHolder(repository, scope)
    }
}
