package com.luum.michi.app.notifications.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.notifications.domain.NotificationsRepository
import com.luum.michi.app.notifications.domain.model.AppNotification
import com.luum.michi.app.notifications.domain.model.NotificationFilter
import com.luum.michi.app.ui.state.LatestRequest
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope
import kotlin.time.TimeSource

internal class NotificationsStateHolder(
    private val repository: NotificationsRepository,
    private val scope: CoroutineScope,
) {
    /** Pages of the current filter: append/dedup/cancel live in [PagedList]. */
    private val paginated = PagedList<AppNotification> { a, b -> (a + b).distinctBy { it.id } }

    /** Bumped on every (re)load: a page or first page from the previous one never lands. */
    private var generation = 0
    private val loadRequest = LatestRequest()

    /** True once page 1 answered: [onOpen] only re-asks when there is a reason. */
    private var hasLoaded by mutableStateOf(false)
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null

    val notifications: List<AppNotification> get() = paginated.items
    val isLoadingMore: Boolean get() = paginated.isLoadingMore
    val hasNextPage: Boolean get() = paginated.hasNextPage
    /** Current page of the backing PagedList: the auto-paging effect keys on it. */
    val currentPage: Int get() = paginated.currentPage

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<NetworkError?>(null)
        private set
    /** Non-blocking failure of a page load: the loaded feed stays on screen. */
    var actionError by mutableStateOf<NetworkError?>(null)
        private set
    var unreadCount by mutableStateOf(0)
        private set
    var selectedFilter by mutableStateOf(NotificationFilter.ALL)
        private set

    /**
     * The unread dot is owned here, fed by the profile (which carries the
     * Viewer's count on its own query, 0 extra requests). A null from the profile
     * cache never reaches this (the caller only forwards non-null counts), so a
     * cold start keeps whatever the last known value was.
     */
    fun updateUnreadCount(count: Int) {
        unreadCount = count
    }

    /**
     * Opening the page is reading it, so it resets the server count. Skipped only
     * when it was already read and the feed is still fresh: no reason, no request.
     */
    fun onOpen() {
        val mark = lastLoaded
        val fresh = hasLoaded && unreadCount == 0 && mark != null &&
            mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL
        if (fresh) return
        load(resetCount = true)
    }

    /**
     * First page. The loaded items stay on screen until the new page 1 replaces
     * them via [PagedList.set] (pull-to-refresh), except [selectFilter], which
     * empties the list first.
     *
     * [resetCount] marks the feed read: on success the count is forced to 0 even
     * if AniList reports the pre-reset value (the query asks it on page 1 always).
     */
    fun load(resetCount: Boolean = false) {
        invalidate()
        isLoading = true
        error = null
        actionError = null
        val filter = selectedFilter
        loadRequest.launch(
            scope = scope,
            onSettled = { isLoading = false },
        ) { isCurrent ->
            when (val result = repository.loadNotifications(page = 1, resetCount = resetCount, filter = filter)) {
                is NetworkResult.Success -> if (isCurrent()) {
                    val page = result.value
                    paginated.set(Page(page.notifications, page.hasNextPage, 1))
                    unreadCount = if (resetCount) 0 else page.unreadCount ?: unreadCount
                    hasLoaded = true
                    lastLoaded = timeMark.markNow()
                    error = null
                }
                is NetworkResult.Failure -> if (isCurrent()) error = result.error
            }
        }
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
            onLoaded = { actionError = null },
            onError = { actionError = it },
        )
    }

    /** Dismisses the compact action-error row. */
    fun clearActionError() {
        actionError = null
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
