package com.luum.michi.app.mediaList.ui.common.state

import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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

    private var loadJob: Job? = null
    private var inFlightUserId: Int? = null
    // Bumped per launch so a cancelled load's `finally` never clears the
    // indicators of the load that replaced it.
    private var loadGeneration = 0

    val entries: List<T> get() = entriesBacking
    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState
    val error: NetworkError? get() = errorState

    fun load(userId: Int, forceRefresh: Boolean = false) {
        val mark = lastLoaded
        if (!forceRefresh && lastUserId == userId && mark != null
            && mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL && entriesBacking.isNotEmpty()
        ) return
        val inFlight = loadJob
        if (inFlight?.isActive == true) {
            // Same request already running: join it (existing behaviour). A
            // forced reload or a different user supersedes it — cancel and
            // relaunch, so a stale response can never win.
            if (inFlightUserId == userId && !forceRefresh) return
            inFlight.cancel()
        }
        val isRefresh = forceRefresh && entriesBacking.isNotEmpty()
        loadingState = !isRefresh
        refreshingState = isRefresh
        errorState = null
        inFlightUserId = userId
        val gen = ++loadGeneration
        loadJob = scope.launch {
            try {
                when (val result = fetch(userId)) {
                    is NetworkResult.Success -> {
                        entriesBacking.clear()
                        entriesBacking.addAll(result.value)
                        lastLoaded = timeMark.markNow()
                        lastUserId = userId
                        onReplaced?.invoke()
                    }
                    is NetworkResult.Failure -> {
                        errorState = result.error
                    }
                }
            } finally {
                if (gen == loadGeneration) {
                    loadingState = false
                    refreshingState = false
                    inFlightUserId = null
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
