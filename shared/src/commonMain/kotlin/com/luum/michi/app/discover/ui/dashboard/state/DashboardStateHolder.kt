package com.luum.michi.app.discover.ui.dashboard.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.discover.domain.DashboardFeed
import com.luum.michi.app.discover.domain.DashboardRepository
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

internal class DashboardStateHolder(
    private val repository: DashboardRepository,
    private val scope: CoroutineScope,
    private val strings: LanguageStrings,
) {
    // Selected selector value; loads the other type on switch (cached per type).
    var mediaType: DashboardMediaType by mutableStateOf(DashboardMediaType.ANIME)
        private set

    // Snapshot map so a completed load recomposes the screen.
    private val feeds = mutableStateMapOf<DashboardMediaType, DashboardFeed>()
    private val lastLoaded = mutableMapOf<DashboardMediaType, TimeSource.Monotonic.ValueTimeMark>()
    private val timeMark = TimeSource.Monotonic

    private var loadingState: Boolean by mutableStateOf(false)
    private var refreshingState: Boolean by mutableStateOf(false)
    private var errorState: NetworkError? by mutableStateOf(null)

    // 18+ gate, owned here so a change can force a reload. Hidden by default
    // (matches the settings default); `false` lets the server return both.
    private var hideAdultState: Boolean = true

    // A force-refresh that arrives while another load is in flight would be
    // dropped; remember it so the in-flight (possibly stale) response is
    // followed by a reload with the value in effect at that point.
    private var pendingReload = false

    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState
    val error: NetworkError? get() = errorState

    /** Rails for the current [mediaType]; empty until its first load. */
    val sections: Map<DashboardSectionKind, List<MediaItem>>
        get() = feeds[mediaType]?.sections ?: emptyMap()

    /** Selector: switching to an unloaded type fetches it (TTL caches revisits). */
    fun selectType(type: DashboardMediaType) {
        if (mediaType == type) return
        mediaType = type
        load()
    }

    /** Mirror the 18+ setting; a change forces a reload so the feed matches. */
    fun setHideAdult(hideAdult: Boolean) {
        if (hideAdultState == hideAdult) return
        hideAdultState = hideAdult
        load(forceRefresh = true)
    }

    /** Load the current type's rails, skipping the network within the TTL. */
    fun load(forceRefresh: Boolean = false) {
        val type = mediaType
        val hasData = feeds[type]?.sections?.isNotEmpty() == true
        val mark = lastLoaded[type]
        if (!forceRefresh && mark != null && mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL && hasData) return
        // Drop overlapping loads so a slower (stale) response cannot overwrite
        // a newer one; a forced one is deferred instead of lost.
        if (loadingState || refreshingState) {
            if (forceRefresh) pendingReload = true
            return
        }
        val isRefresh = forceRefresh && hasData
        if (isRefresh) {
            refreshingState = true
        } else {
            loadingState = true
        }
        errorState = null
        scope.launch {
            try {
                when (val result = repository.loadFeed(type, hideAdultState, strings)) {
                    is NetworkResult.Success -> {
                        feeds[type] = result.value
                        lastLoaded[type] = timeMark.markNow()
                    }
                    is NetworkResult.Failure -> errorState = result.error
                }
            } finally {
                loadingState = false
                refreshingState = false
                // A refresh was requested mid-flight: run it now so the final
                // state reflects the newest selection/hideAdult, not the stale one.
                if (pendingReload) {
                    pendingReload = false
                    load(forceRefresh = true)
                }
            }
        }
    }
}

/** Pure-logic factory. Loading is explicit (the screen triggers it), so
 *  creating a holder never fires a request. */
internal fun createDashboardStateHolder(
    repository: DashboardRepository,
    scope: CoroutineScope,
    strings: LanguageStrings,
): DashboardStateHolder {
    return DashboardStateHolder(repository, scope, strings)
}
