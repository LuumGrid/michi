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
import com.luum.michi.app.feed.data.FeedChip
import com.luum.michi.app.feed.data.FeedFilter
import com.luum.michi.app.feed.data.FeedRepository
import com.luum.michi.app.feed.data.FeedSection
import com.luum.michi.app.feed.presentation.model.FeedActivity
import com.luum.michi.app.feed.presentation.model.FeedReview
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
    // — Section selector —
    var section by mutableStateOf(FeedSection.ACTIVITY)
        private set

    fun selectSection(newSection: FeedSection) {
        if (section == newSection) return
        section = newSection
        if (newSection == FeedSection.REVIEWS && reviewsBacking.isEmpty()) {
            loadReviews()
        }
    }

    // — Activity state —
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

    // — Reviews state —
    private val reviewsBacking = mutableStateListOf<FeedReview>()
    val reviews: List<FeedReview> get() = reviewsBacking

    var reviewsHasNextPage by mutableStateOf(false)
        private set
    var isLoadingReviews by mutableStateOf(false)
        private set
    var isLoadingMoreReviews by mutableStateOf(false)
        private set
    var reviewsError by mutableStateOf<String?>(null)
        private set

    private var reviewsCurrentPage = 1
    private var reviewsJob: Job? = null
    private var reviewsLoadMoreJob: Job? = null

    val selectedChip: FeedChip
        get() = when {
            section == FeedSection.REVIEWS -> FeedChip.REVIEWS
            filter == FeedFilter.FOLLOWING -> FeedChip.FOLLOWING
            else -> FeedChip.GLOBAL
        }

    fun selectChip(chip: FeedChip) {
        when (chip) {
            FeedChip.REVIEWS -> {
                if (section != FeedSection.REVIEWS) {
                    section = FeedSection.REVIEWS
                    if (reviewsBacking.isEmpty()) loadReviews()
                }
            }
            FeedChip.FOLLOWING, FeedChip.GLOBAL -> {
                val newFilter = if (chip == FeedChip.FOLLOWING) FeedFilter.FOLLOWING else FeedFilter.GLOBAL
                val wasReviews = section == FeedSection.REVIEWS
                section = FeedSection.ACTIVITY
                if (filter != newFilter) {
                    filter = newFilter
                    resetState()
                    load(forceRefresh = true)
                } else if (wasReviews && backing.isEmpty()) {
                    load(forceRefresh = true)
                }
            }
        }
    }

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

    fun loadReviews(forceRefresh: Boolean = false) {
        if (!forceRefresh && reviewsBacking.isNotEmpty()) return
        reviewsJob?.cancel()
        reviewsLoadMoreJob?.cancel()
        reviewsBacking.clear()
        reviewsCurrentPage = 1
        reviewsHasNextPage = false
        reviewsError = null
        isLoadingReviews = true
        reviewsJob = scope.launch {
            try {
                when (val result = repository.loadReviews(page = 1)) {
                    is NetworkResult.Success -> {
                        val page = result.value
                        reviewsBacking.addAll(page.reviews)
                        reviewsHasNextPage = page.hasNextPage
                        reviewsCurrentPage = 1
                        reviewsError = null
                    }
                    is NetworkResult.Failure -> {
                        reviewsError = result.error.toString()
                    }
                }
            } finally {
                isLoadingReviews = false
            }
        }
    }

    fun loadMoreReviews() {
        if (isLoadingMoreReviews || !reviewsHasNextPage) return
        val nextPage = reviewsCurrentPage + 1
        isLoadingMoreReviews = true
        reviewsLoadMoreJob = scope.launch {
            when (val result = repository.loadReviews(page = nextPage)) {
                is NetworkResult.Success -> {
                    val page = result.value
                    reviewsBacking.addAll(page.reviews)
                    reviewsHasNextPage = page.hasNextPage
                    reviewsCurrentPage = nextPage
                }
                is NetworkResult.Failure -> {
                    reviewsError = result.error.toString()
                }
            }
            isLoadingMoreReviews = false
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
