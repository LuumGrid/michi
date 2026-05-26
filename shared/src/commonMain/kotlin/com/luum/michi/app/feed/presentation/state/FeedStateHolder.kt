package com.luum.michi.app.feed.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.feed.data.FeedActivityFilter
import com.luum.michi.app.feed.data.FeedFilter
import com.luum.michi.app.feed.data.FeedRepository
import com.luum.michi.app.feed.presentation.model.FeedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

internal class FeedStateHolder(
    private val repository: FeedRepository,
    private val viewerId: Int,
    private val scope: CoroutineScope,
) {
    var filter by mutableStateOf(FeedFilter.FOLLOWING)
        private set
    var activityFilter by mutableStateOf(FeedActivityFilter())
        private set

    private val backing = mutableStateListOf<FeedActivity>()
    val activities: List<FeedActivity> get() = backing

    var isLoading by mutableStateOf(false)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var hasNextPage by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var currentPage = 1
    private var currentJob: Job? = null
    private var loadMoreJob: Job? = null
    private var smallestIdSeen: Int? = null

    private val timeMark = TimeSource.Monotonic
    private val lastLoadedByFilter = mutableMapOf<FeedFilter, TimeSource.Monotonic.ValueTimeMark>()

    fun selectFilter(newFilter: FeedFilter) {
        if (filter == newFilter) return
        filter = newFilter
        resetState()
        load(forceRefresh = true)
    }

    fun applyActivityFilter(newFilter: FeedActivityFilter) {
        activityFilter = newFilter
        lastLoadedByFilter.clear()
        load(forceRefresh = true)
    }

    fun load(forceRefresh: Boolean = false) {
        val mark = lastLoadedByFilter[filter]
        if (!forceRefresh && mark != null && mark.elapsedNow() < CACHE_TTL && backing.isNotEmpty()) return
        val isRefresh = forceRefresh && backing.isNotEmpty()
        currentJob?.cancel()
        loadMoreJob?.cancel()
        resetState()
        if (isRefresh) isRefreshing = true else isLoading = true
        currentJob = scope.launch {
            try {
                when (val result = repository.loadFeed(filter, activityFilter, page = 1, viewerId = viewerId)) {
                    is NetworkResult.Success -> {
                        val page = result.value
                        backing.addAll(page.activities)
                        smallestIdSeen = page.activities.minOfOrNull { it.id }
                        hasNextPage = page.hasNextPage
                        currentPage = 1
                        lastLoadedByFilter[filter] = timeMark.markNow()
                        error = null
                    }
                    is NetworkResult.Failure -> {
                        error = result.error.toString()
                    }
                }
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasNextPage) return
        val nextPage = currentPage + 1
        isLoadingMore = true
        loadMoreJob = scope.launch {
            when (val result = repository.loadFeed(filter, activityFilter, page = nextPage, viewerId = viewerId)) {
                is NetworkResult.Success -> {
                    val page = result.value
                    val currentSmallest = smallestIdSeen
                    val incoming = if (currentSmallest != null) {
                        page.activities.filter { it.id < currentSmallest }
                    } else {
                        page.activities
                    }
                    backing.addAll(incoming)
                    if (incoming.isNotEmpty()) {
                        smallestIdSeen = minOf(
                            currentSmallest ?: Int.MAX_VALUE,
                            incoming.minOf { it.id },
                        )
                    }
                    hasNextPage = page.hasNextPage
                    currentPage = nextPage
                }
                is NetworkResult.Failure -> {
                    error = result.error.toString()
                }
            }
            isLoadingMore = false
        }
    }

    private fun resetState() {
        backing.clear()
        currentPage = 1
        hasNextPage = false
        error = null
        smallestIdSeen = null
    }

    companion object {
        private val CACHE_TTL = 5.minutes
    }
}

@Composable
internal fun rememberFeedStateHolder(
    repository: FeedRepository,
    viewerId: Int,
): FeedStateHolder {
    val scope = rememberCoroutineScope()
    return remember(viewerId) {
        FeedStateHolder(repository, viewerId, scope)
    }
}
