package com.luum.michi.app.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * One paginated list shared by the detail holders (media characters/staff/
 * recommendations, character/staff/studio works, genre works). Owns the Compose
 * state and the page-in-flight job, so a holder only writes the network call.
 *
 * [merge] folds a freshly fetched page into the loaded items (dedup lives here);
 * it is applied to the first page too. A page is only applied while the request's
 * `isCurrent()` still holds, so a page from the previous entity/sort never lands.
 *
 * [set] seeds the first page (the holders that get it inside the detail
 * response); [loadFirst] replaces it from its own call (recommendations);
 * [loadMore] appends `currentPage + 1` and stops until [set]/[loadFirst] started.
 */
internal class PagedList<T>(
    private val merge: (List<T>, List<T>) -> List<T> = { a, b -> a + b },
) {
    var items: List<T> by mutableStateOf(emptyList())
        private set
    var hasNextPage: Boolean by mutableStateOf(false)
        private set
    var currentPage: Int by mutableStateOf(1)
        private set
    var isLoadingMore: Boolean by mutableStateOf(false)
        private set

    private var job: Job? = null

    /** A page has been set/loaded: [loadMore] advances instead of starting at 1. */
    private var started = false

    /** Replaces the whole list with [page] (first page or cache restore). */
    fun set(page: Page<T>) {
        cancel()
        items = merge(emptyList(), page.items)
        hasNextPage = page.hasNextPage
        currentPage = page.currentPage
        started = true
    }

    /** Empties the list back to page 1 (new entity/sort, or a cache miss). */
    fun clear() {
        cancel()
        items = emptyList()
        hasNextPage = false
        currentPage = 1
        started = false
    }

    /** The current list as a [Page], for the holder's cache snapshot. */
    fun snapshot(): Page<T> = Page(items = items, hasNextPage = hasNextPage, currentPage = currentPage)

    /** Cancels a page in flight and drops its spinner (never appends it). */
    fun cancel() {
        job?.cancel()
        job = null
        isLoadingMore = false
    }

    /**
     * Loads page 1 and replaces the list. Only the recommendations list uses it;
     * the other holders seed their first page from the detail response via [set].
     */
    fun loadFirst(
        scope: CoroutineScope,
        isCurrent: () -> Boolean,
        fetch: suspend () -> NetworkResult<Page<T>>,
        onLoaded: () -> Unit = {},
        onError: (NetworkError) -> Unit = {},
    ) {
        if (isLoadingMore) return
        request(scope) {
            when (val result = fetch()) {
                is NetworkResult.Success -> if (isCurrent()) {
                    items = merge(emptyList(), result.value.items)
                    hasNextPage = result.value.hasNextPage
                    currentPage = result.value.currentPage
                    started = true
                    onLoaded()
                }
                is NetworkResult.Failure -> if (isCurrent()) onError(result.error)
            }
        }
    }

    fun loadMore(
        scope: CoroutineScope,
        isCurrent: () -> Boolean,
        fetch: suspend (page: Int) -> NetworkResult<Page<T>>,
        onLoaded: () -> Unit = {},
        onError: (NetworkError) -> Unit = {},
    ) {
        if (isLoadingMore || !started || !hasNextPage) return
        val nextPage = currentPage + 1
        request(scope) {
            when (val result = fetch(nextPage)) {
                is NetworkResult.Success -> if (isCurrent()) {
                    items = merge(items, result.value.items)
                    hasNextPage = result.value.hasNextPage
                    currentPage = result.value.currentPage
                    onLoaded()
                }
                is NetworkResult.Failure -> if (isCurrent()) onError(result.error)
            }
        }
    }

    /**
     * Runs a request and owns the spinner. Only the request the list still tracks
     * clears [isLoadingMore] — a holder that swapped entity without a
     * cancel/clear must not leave it stuck. Launched LAZY and then started so the
     * job is stored before the body runs: the identity check is always valid even
     * on an eagerly-dispatched scope.
     */
    private fun request(scope: CoroutineScope, block: suspend () -> Unit) {
        isLoadingMore = true
        job?.cancel()
        val request = scope.launch(start = CoroutineStart.LAZY) {
            block()
            if (job === coroutineContext[Job]) isLoadingMore = false
        }
        job = request
        request.start()
    }
}
