package com.luum.michi.app.profile.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.domain.model.ProfileReview
import com.luum.michi.app.ui.state.LatestRequest
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope
import kotlin.time.TimeSource

/**
 * Paginated state for the viewer's reviews (newest first). Public AniList data,
 * memory-only within the shared 5-minute TTL: re-opening costs 0 requests, a
 * cold open one, and paging one per page.
 */
internal class ProfileReviewsStateHolder(
    private val repository: ProfileRepository,
    private val scope: CoroutineScope,
) {
    private val paginated = PagedList<ProfileReview> { a, b -> (a + b).distinctBy { it.id } }

    /** Bumped on every new load: a page from the previous one never lands. */
    private var generation = 0
    private val loadRequest = LatestRequest()

    private var hasLoaded by mutableStateOf(false)
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null

    private var loadedUserId: Int? = null
    /** Last user a load was started for: [refresh] retries it even after a failure. */
    private var requestedUserId: Int? = null

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<NetworkError?>(null)
        private set
    /** Non-blocking failure of a page load: the loaded list stays on screen. */
    var actionError by mutableStateOf<NetworkError?>(null)
        private set

    val reviews: List<ProfileReview> get() = paginated.items
    val isLoadingMore: Boolean get() = paginated.isLoadingMore
    val hasNextPage: Boolean get() = paginated.hasNextPage
    val currentPage: Int get() = paginated.currentPage

    fun load(userId: Int) {
        requestedUserId = userId
        val mark = lastLoaded
        if (hasLoaded && loadedUserId == userId && mark != null &&
            mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL
        ) return
        startFirstPage(userId)
    }

    /** Retry after a failure / pull-to-refresh: re-runs the requested page 1. */
    fun refresh() {
        requestedUserId?.let(::startFirstPage)
    }

    fun loadMore() {
        val userId = loadedUserId ?: return
        if (isLoading) return
        val currentGeneration = generation
        paginated.loadMore(
            scope = scope,
            isCurrent = { generation == currentGeneration },
            fetch = { page -> repository.loadReviewsPage(userId, page) },
            onLoaded = { actionError = null },
            onError = { actionError = it },
        )
    }

    fun clearActionError() {
        actionError = null
    }

    private fun startFirstPage(userId: Int) {
        generation++
        paginated.cancel()
        isLoading = true
        error = null
        actionError = null
        loadRequest.launch(
            scope = scope,
            onSettled = { isLoading = false },
        ) { isCurrent ->
            when (val result = repository.loadReviewsPage(userId, page = 1)) {
                is NetworkResult.Success -> if (isCurrent()) {
                    paginated.set(result.value)
                    loadedUserId = userId
                    hasLoaded = true
                    lastLoaded = timeMark.markNow()
                }
                is NetworkResult.Failure -> if (isCurrent()) {
                    error = result.error
                    loadedUserId = null
                    hasLoaded = false
                }
            }
        }
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createProfileReviewsStateHolder(
    repository: ProfileRepository,
    scope: CoroutineScope,
): ProfileReviewsStateHolder = ProfileReviewsStateHolder(repository, scope)
