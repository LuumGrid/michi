package com.luum.michi.app.mediaList.ui.common.state

import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.ui.state.LatestRequest
import kotlinx.coroutines.CoroutineScope
import kotlin.time.TimeSource

/**
 * Shared fetch/cache machinery for the anime/manga user lists. Both lists are
 * full `MediaListCollection`s fetched once (no server pagination): the holder
 * keeps TTL + pull-to-refresh state here and only its sort/increment/status
 * mapping, which genuinely differ per list.
 *
 * Composition, not inheritance: each `*ListStateHolder` owns one of these
 * typed by its entry. The TTL mirrors the previous per-holder logic 1:1.
 *
 * UI-visible state (entries, loading, refreshing, error) is snapshot-backed
 * so async completions recompose on their own — plain vars would leave the
 * screen stale until an unrelated state change re-reads them. TTL bookkeeping
 * stays plain: the UI never reads it.
 */
internal class MediaListLoader<T>(
    private val scope: CoroutineScope,
    private val fetch: suspend (userId: Int) -> NetworkResult<List<T>>,
) {
    private val entriesBacking = mutableStateListOf<T>()
    private var loadingState by mutableStateOf(false)
    private var refreshingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null
    private var lastUserId: Int? = null

    /**
     * Fired when a successful load replaces the entries (never on a TTL skip or
     * a join of an in-flight request). The list's `+1` rollback/error reset runs
     * off this, so a tab switch that loads nothing must not wipe it.
     */
    var onReplaced: (() -> Unit)? = null

    private val loadRequest = LatestRequest()
    private var inFlightUserId: Int? = null

    val entries: List<T> get() = entriesBacking
    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState
    val error: NetworkError? get() = errorState

    fun load(userId: Int, forceRefresh: Boolean = false) {
        val mark = lastLoaded
        if (!forceRefresh && lastUserId == userId && mark != null
            && mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL && entriesBacking.isNotEmpty()
        ) return
        // Same request already running: join it. A forced reload or a different
        // user supersedes it — the helper cancels and relaunches.
        if (loadRequest.isActive && inFlightUserId == userId && !forceRefresh) return
        val isRefresh = forceRefresh && entriesBacking.isNotEmpty()
        loadingState = !isRefresh
        refreshingState = isRefresh
        errorState = null
        inFlightUserId = userId
        // Only the current load writes its entries/error and owns the
        // indicators; a superseded one never lands.
        loadRequest.launch(
            scope = scope,
            onSettled = {
                loadingState = false
                refreshingState = false
                inFlightUserId = null
            },
        ) { isCurrent ->
            when (val result = fetch(userId)) {
                is NetworkResult.Success -> if (isCurrent()) {
                    entriesBacking.clear()
                    entriesBacking.addAll(result.value)
                    lastLoaded = timeMark.markNow()
                    lastUserId = userId
                    onReplaced?.invoke()
                }
                is NetworkResult.Failure -> if (isCurrent()) {
                    errorState = result.error
                }
            }
        }
    }

    fun indexOf(predicate: (T) -> Boolean): Int = entriesBacking.indexOfFirst(predicate)

    operator fun get(index: Int): T = entriesBacking[index]

    operator fun set(index: Int, value: T) {
        entriesBacking[index] = value
    }
}
