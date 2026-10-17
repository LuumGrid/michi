package com.luum.michi.app.mediaDetail.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.navigation.domain.DetailHolder
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.util.DetailCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Shared lifecycle of the four detail holders (media, character, staff, studio):
 * the visible [detail] and its load/refresh switch, the five-entry LRU snapshot
 * cache and the optimistic favourite toggle. A subclass only wires its own
 * network calls and its own `PagedList`s — the base never knows about them.
 *
 * [D] is the detail model, [S] its cache snapshot. The hooks are the minimum a
 * scope must supply; [onCacheMiss]/[onRefreshed] cover the studio's default sort
 * and the media's per-visit recommendations.
 */
internal abstract class DetailStateHolderBase<D, S>(
    protected val scope: CoroutineScope,
) : DetailHolder {
    // Snapshot-backed: the screens read these directly and must recompose on
    // every load/pagination (plain vars would apply silently).
    var detail: D? by mutableStateOf(null)
        protected set
    var isLoading: Boolean by mutableStateOf(false)
        protected set
    var error: NetworkError? by mutableStateOf(null)
        protected set
    /** Non-blocking action failure (a page load or a favourite toggle): shown as
     *  a compact row while the content stays visible, never as the load error. */
    var actionError: NetworkError? by mutableStateOf(null)
        protected set

    private var currentId: Int? = null
    private var currentJob: Job? = null

    /** Cache of up to 5 recently-visited detail pages. */
    private val detailCache = DetailCache<Int, S>(maxSize = 5)

    // Per id: a toggle in flight for one entity must not disable (or reflect on)
    // another one's heart.
    private var favouriteInFlightId: Int? by mutableStateOf(null)
    val isTogglingFavourite: Boolean
        get() = favouriteInFlightId != null && favouriteInFlightId == currentId

    /** The id currently bound, for the subclasses' pagination guards. */
    protected val currentDetailId: Int? get() = currentId

    fun load(id: Int) {
        if (currentId == id && (detail != null || isLoading)) return

        // Restore from the LRU cache when a valid snapshot exists (no network).
        val cached = detailCache.get(id)
        if (cached != null && isSnapshotValid(cached)) {
            currentId = id
            currentJob?.cancel()
            // Pages in flight belong to what was on screen: never append them to
            // the restored snapshot (set/clear inside restore cancels them).
            restore(cached)
            error = null
            actionError = null
            isLoading = false
            return
        }

        currentId = id
        onCacheMiss(id)
        fetchFirstPage(id, clearDetail = true, clearLists = true)
    }

    open override fun refresh() {
        val id = currentId ?: return
        // Invalidate the cached entry so the next visit re-fetches from network.
        detailCache.remove(id)
        // Keep the detail and lists on screen while reloading (no flicker).
        launchFetch(
            id,
            clearDetail = false,
            clearLists = false,
            // Only while this refresh is still the current one (a later load
            // cancels the job, but the guard mirrors the original behaviour).
            afterSuccess = { if (currentId == id) onRefreshed(id) },
        )
    }

    override fun toggleFavourite() {
        val current = detail ?: return
        val id = currentId ?: return
        if (favouriteInFlightId == id) return
        val previous = isFavourite
        val target = !previous
        favouriteInFlightId = id
        // Optimistic: the visible detail and its cached snapshot both flip now,
        // so a revisit from the cache never restores the stale value.
        detail = withFavourite(current, target)
        updateCachedFavourite(id, target)
        scope.launch {
            when (val result = requestToggleFavourite(id, current)) {
                is NetworkResult.Success -> {
                    if (currentId == id) detail = detail?.let { withFavourite(it, target) }
                }
                is NetworkResult.Failure -> {
                    // Revert both copies of THIS entity's value; the visible page
                    // and its error only while it is still the current one.
                    updateCachedFavourite(id, previous)
                    if (currentId == id) {
                        detail = detail?.let { withFavourite(it, previous) }
                        actionError = result.error
                    }
                }
            }
            if (favouriteInFlightId == id) favouriteInFlightId = null
        }
    }

    /** Drops every cached snapshot: a language change re-localizes all of them. */
    override fun invalidateCache() {
        detailCache.clear()
    }

    /** Dismisses the compact action-error row. */
    fun clearActionError() {
        actionError = null
    }

    /** Fetches page 1 through [fetchDetail]; the single network path of a holder. */
    protected fun fetchFirstPage(id: Int, clearDetail: Boolean, clearLists: Boolean) {
        launchFetch(id, clearDetail, clearLists, afterSuccess = null)
    }

    private fun launchFetch(
        id: Int,
        clearDetail: Boolean,
        clearLists: Boolean,
        afterSuccess: (() -> Unit)?,
    ) {
        currentJob?.cancel()
        // A page from the previous entity/sort must never append.
        cancelPages()
        if (clearDetail) detail = null
        if (clearLists) clearPages()
        error = null
        actionError = null
        isLoading = true
        currentJob = scope.launch {
            when (val result = fetchDetail(id)) {
                is NetworkResult.Success -> {
                    detail = result.value
                    onDetailLoaded(result.value)
                    cacheSnapshot(id)
                    afterSuccess?.invoke()
                }
                is NetworkResult.Failure -> error = result.error
            }
            isLoading = false
        }
    }

    /** A page finished: clears the compact error and re-caches the snapshot. */
    protected fun pageLoaded(id: Int) {
        actionError = null
        cacheSnapshot(id)
    }

    /** A page failed: shows the compact, non-blocking error row. */
    protected fun pageError(error: NetworkError) {
        actionError = error
    }

    /** Drops one cached snapshot (an editor save/delete invalidates its entry). */
    protected fun dropCached(id: Int) {
        detailCache.remove(id)
    }

    /** Flips one cached snapshot's favourite without touching the visible page. */
    protected fun updateCachedFavourite(id: Int, value: Boolean) {
        detailCache.update(id) { snapshotWithFavourite(it, value) }
    }

    private fun cacheSnapshot(id: Int) {
        val value = detail ?: return
        detailCache.put(id, snapshot(value))
    }

    /** The snapshot for [detail] plus the pages the subclass has loaded. */
    protected abstract fun snapshot(detail: D): S

    /** Restores [detail] and the pages from a cached [snapshot]. */
    protected abstract fun restore(snapshot: S)

    /** Whether a cached snapshot can still be restored (character: same cast). */
    protected open fun isSnapshotValid(snapshot: S): Boolean = true

    /** The network call behind page 1 (each scope builds its own arguments). */
    protected abstract suspend fun fetchDetail(id: Int): NetworkResult<D>

    /** Applies the first page's payload to the subclass lists. */
    protected abstract fun onDetailLoaded(detail: D)

    /** Cancels any page in flight (never append it to a new entity/snapshot). */
    protected abstract fun cancelPages()

    /** Empties the subclass lists (a new entity or sort). */
    protected abstract fun clearPages()

    /** [detail] with its favourite flag set to [value]. */
    protected abstract fun withFavourite(detail: D, value: Boolean): D

    /** [snapshot]'s detail with its favourite flag set to [value]. */
    protected abstract fun snapshotWithFavourite(snapshot: S, value: Boolean): S

    /** The favourite mutation for this scope (media passes its `isManga`). */
    protected abstract suspend fun requestToggleFavourite(id: Int, detail: D): NetworkResult<Unit>

    /** Called on a cache miss before the first fetch (studio resets its sort). */
    protected open fun onCacheMiss(id: Int) {}

    /** Called after a refresh's fetch succeeds while [id] is still current. */
    protected open fun onRefreshed(id: Int) {}
}
