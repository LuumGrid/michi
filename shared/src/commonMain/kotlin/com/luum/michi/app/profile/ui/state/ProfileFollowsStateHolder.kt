package com.luum.michi.app.profile.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.domain.model.ProfileFollow
import com.luum.michi.app.profile.domain.model.ProfileFollowsKind
import com.luum.michi.app.ui.state.LatestRequest
import com.luum.michi.app.ui.state.PagedList
import kotlinx.coroutines.CoroutineScope
import kotlin.time.TimeSource

/**
 * Paginated state for the viewer's following/followers list. Public AniList
 * data, cached in memory only within the shared 5-minute TTL: re-opening the
 * same kind costs 0 requests, a cold open costs one, and paging costs one per
 * page. Switching kind drops the previous list at once instead of leaving it
 * under the spinner.
 */
internal class ProfileFollowsStateHolder(
    private val repository: ProfileRepository,
    private val scope: CoroutineScope,
) {
    private val paginated = PagedList<ProfileFollow> { a, b -> (a + b).distinctBy { it.id } }

    /** Bumped on every new load: a page from the previous one never lands. */
    private var generation = 0
    private val loadRequest = LatestRequest()

    private var hasLoaded by mutableStateOf(false)
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null

    private var loadedUserId: Int? = null
    private var loadedKind: ProfileFollowsKind? = null
    /** Last (userId, kind) a load was started for: [refresh] retries it. */
    private var requestedUserId: Int? = null
    private var requestedKind: ProfileFollowsKind? = null

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<NetworkError?>(null)
        private set
    /** Non-blocking failure of a page load: the loaded list stays on screen. */
    var actionError by mutableStateOf<NetworkError?>(null)
        private set

    val follows: List<ProfileFollow> get() = paginated.items
    val isLoadingMore: Boolean get() = paginated.isLoadingMore
    val hasNextPage: Boolean get() = paginated.hasNextPage
    val currentPage: Int get() = paginated.currentPage

    fun load(userId: Int, kind: ProfileFollowsKind) {
        requestedUserId = userId
        requestedKind = kind
        val sameKey = hasLoaded && loadedUserId == userId && loadedKind == kind
        val mark = lastLoaded
        if (sameKey && mark != null && mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL) return
        // A different kind is a new list: drop the previous one at once.
        if (!sameKey) {
            paginated.clear()
            hasLoaded = false
        }
        startFirstPage(userId, kind)
    }

    /** Retry after a failure / pull-to-refresh: re-runs the requested page 1. */
    fun refresh() {
        val userId = requestedUserId ?: return
        val kind = requestedKind ?: return
        startFirstPage(userId, kind)
    }

    fun loadMore() {
        val userId = loadedUserId ?: return
        val kind = loadedKind ?: return
        if (isLoading) return
        val currentGeneration = generation
        paginated.loadMore(
            scope = scope,
            isCurrent = { generation == currentGeneration },
            fetch = { page -> repository.loadFollowsPage(userId, kind, page) },
            onLoaded = { actionError = null },
            onError = { actionError = it },
        )
    }

    fun clearActionError() {
        actionError = null
    }

    private fun startFirstPage(userId: Int, kind: ProfileFollowsKind) {
        generation++
        paginated.cancel()
        isLoading = true
        error = null
        actionError = null
        loadRequest.launch(
            scope = scope,
            onSettled = { isLoading = false },
        ) { isCurrent ->
            when (val result = repository.loadFollowsPage(userId, kind, page = 1)) {
                is NetworkResult.Success -> if (isCurrent()) {
                    paginated.set(result.value)
                    loadedUserId = userId
                    loadedKind = kind
                    hasLoaded = true
                    lastLoaded = timeMark.markNow()
                }
                is NetworkResult.Failure -> if (isCurrent()) {
                    error = result.error
                    // A failed first page leaves nothing to page from: a later
                    // load() must not be swallowed by the same-key early return.
                    loadedUserId = null
                    loadedKind = null
                    hasLoaded = false
                }
            }
        }
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createProfileFollowsStateHolder(
    repository: ProfileRepository,
    scope: CoroutineScope,
): ProfileFollowsStateHolder = ProfileFollowsStateHolder(repository, scope)
