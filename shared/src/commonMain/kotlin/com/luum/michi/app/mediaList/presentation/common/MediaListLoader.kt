package com.luum.michi.app.mediaList.presentation.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * Shared fetch/cache machinery for the anime/manga user lists. Both lists are
 * full `MediaListCollection`s fetched once (no server pagination): the holder
 * keeps TTL + pull-to-refresh state here and only its sort/increment/status
 * mapping, which genuinely differ per list.
 *
 * Composition, not inheritance: each `*ListStateHolder` owns one of these
 * typed by its entry. The TTL mirrors the previous per-holder logic 1:1.
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

    val entries: List<T> get() = entriesBacking
    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState
    val error: NetworkError? get() = errorState

    fun load(userId: Int, forceRefresh: Boolean = false) {
        val mark = lastLoaded
        if (!forceRefresh && lastUserId == userId && mark != null
            && mark.elapsedNow() < CACHE_TTL && entriesBacking.isNotEmpty()
        ) return
        val isRefresh = forceRefresh && entriesBacking.isNotEmpty()
        scope.launch {
            if (isRefresh) refreshingState = true else loadingState = true
            errorState = null
            try {
                when (val result = fetch(userId)) {
                    is NetworkResult.Success -> {
                        entriesBacking.clear()
                        entriesBacking.addAll(result.value)
                        lastLoaded = timeMark.markNow()
                        lastUserId = userId
                    }
                    is NetworkResult.Failure -> {
                        errorState = result.error
                    }
                }
            } finally {
                loadingState = false
                refreshingState = false
            }
        }
    }

    fun indexOf(predicate: (T) -> Boolean): Int = entriesBacking.indexOfFirst(predicate)

    operator fun get(index: Int): T = entriesBacking[index]

    operator fun set(index: Int, value: T) {
        entriesBacking[index] = value
    }

    companion object {
        private val CACHE_TTL = 5.minutes
    }
}
