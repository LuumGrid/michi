package com.luum.michi.app.notifications.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.notifications.domain.NotificationsRepository
import com.luum.michi.app.notifications.domain.model.AppNotification
import com.luum.michi.app.notifications.domain.model.NotificationFilter
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class NotificationsStateHolder(
    private val repository: NotificationsRepository,
    private val scope: CoroutineScope,
) {
    /** Pages of the current filter: append/dedup/cancel live in [PagedList]. */
    private val paginated = PagedList<AppNotification> { a, b -> (a + b).distinctBy { it.id } }

    /** Bumped on every (re)load: a page or first page from the previous one never lands. */
    private var generation = 0
    private var currentJob: Job? = null

    val notifications: List<AppNotification> get() = paginated.items
    val isLoadingMore: Boolean get() = paginated.isLoadingMore
    val hasNextPage: Boolean get() = paginated.hasNextPage

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<NetworkError?>(null)
        private set
    var unreadCount by mutableStateOf(0)
        private set
    var selectedFilter by mutableStateOf(NotificationFilter.ALL)
        private set

    /**
     * First page. The loaded items stay on screen until the new page 1 replaces
     * them via [PagedList.set] (pull-to-refresh), except [selectFilter], which
     * empties the list first.
     *
     * [resetCount] marks the feed read: on success the count is forced to 0 even
     * if AniList reports the pre-reset value (the query asks it on page 1 always).
     */
    fun load(resetCount: Boolean = false) {
        currentJob?.cancel()
        invalidate()
        isLoading = true
        error = null
        val filter = selectedFilter
        // LAZY then start, like PagedList.request: the job is stored before the
        // body runs, so the identity check is valid even on an eager scope.
        val request = scope.launch(start = CoroutineStart.LAZY) {
            try {
                when (val result = repository.loadNotifications(page = 1, resetCount = resetCount, filter = filter)) {
                    is NetworkResult.Success -> if (currentJob === coroutineContext[Job]) {
                        val page = result.value
                        paginated.set(Page(page.notifications, page.hasNextPage, 1))
                        unreadCount = if (resetCount) 0 else page.unreadCount ?: unreadCount
                        error = null
                    }
                    is NetworkResult.Failure -> if (currentJob === coroutineContext[Job]) error = result.error
                }
            } finally {
                // Only the current load owns the spinner: a cancelled one must not
                // turn off the load now in flight.
                if (currentJob === coroutineContext[Job]) isLoading = false
            }
        }
        currentJob = request
        request.start()
    }

    fun loadMore() {
        // A load is restarting the list: asking for page N+1 of it would be wrong.
        if (isLoading) return
        val currentGeneration = generation
        val filter = selectedFilter
        paginated.loadMore(
            scope = scope,
            isCurrent = { generation == currentGeneration },
            fetch = { page ->
                repository.loadNotifications(page = page, resetCount = false, filter = filter)
                    .map { Page(it.notifications, it.hasNextPage, page) }
            },
            onError = { error = it },
        )
    }

    /**
     * Switches filter and reloads. A chip tap (not typing), so the previous
     * filter's items are dropped at once instead of lingering under the spinner.
     */
    fun selectFilter(filter: NotificationFilter) {
        if (filter == selectedFilter) return
        selectedFilter = filter
        paginated.clear()
        load()
    }

    /** A new load supersedes the running one: cancel its in-flight page. */
    private fun invalidate() {
        generation++
        paginated.cancel()
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createNotificationsStateHolder(
    repository: NotificationsRepository,
    scope: CoroutineScope,
): NotificationsStateHolder {
    return NotificationsStateHolder(repository, scope)
}
