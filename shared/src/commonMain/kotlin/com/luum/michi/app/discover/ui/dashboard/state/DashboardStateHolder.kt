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
import com.luum.michi.app.discover.domain.DashboardFeedCache
import com.luum.michi.app.discover.domain.DashboardRepository
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.TimeSource

internal class DashboardStateHolder(
    private val repository: DashboardRepository,
    private val cache: DashboardFeedCache,
    private val scope: CoroutineScope,
    private val strings: LanguageStrings,
) {
    // Selected selector value; loads the other type on switch (cached per type).
    var mediaType: DashboardMediaType by mutableStateOf(DashboardMediaType.ANIME)
        private set

    // Snapshot maps so a completed load recomposes the screen.
    private val feeds = mutableStateMapOf<DashboardMediaType, DashboardFeed>()
    private val errors = mutableStateMapOf<DashboardMediaType, NetworkError>()
    private val lastLoaded = mutableMapOf<DashboardMediaType, TimeSource.Monotonic.ValueTimeMark>()
    private val timeMark = TimeSource.Monotonic

    private var loadingState: Boolean by mutableStateOf(false)
    private var refreshingState: Boolean by mutableStateOf(false)

    // 18+ gate, owned here so a change can force a reload. Hidden by default
    // (matches the settings default); `false` lets the server return both.
    private var hideAdultState: Boolean = true

    // A load request that arrives while another is in flight would be dropped;
    // remember it (and whether it was forced) so the in-flight response is
    // followed by a reload with the selection/value in effect at that point.
    private var inFlightType: DashboardMediaType? = null
    private var pendingReload = false
    private var pendingForce = false
    private var preloading = false

    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState

    /** Error of the selected type only: an Anime failure never shows on Manga. */
    val error: NetworkError? get() = errors[mediaType]

    /** Rails for the current [mediaType]; empty until its first load. */
    val sections: Map<DashboardSectionKind, List<MediaItem>>
        get() = feeds[mediaType]?.sections ?: emptyMap()

    /** Selector: switching hydrates from disk (instant) then revalidates. */
    fun selectType(type: DashboardMediaType) {
        if (mediaType == type) return
        mediaType = type
        if (feeds[type] == null) hydrateFromCache(type)
        load()
    }

    /** Mirror the 18+ setting; every rail is gated, so drop the in-memory caches. */
    fun setHideAdult(hideAdult: Boolean) {
        if (hideAdultState == hideAdult) return
        hideAdultState = hideAdult
        feeds.clear()
        lastLoaded.clear()
        errors.clear()
        load(forceRefresh = true)
    }

    /** Load the current type's rails, skipping the network within the TTL. */
    fun load(forceRefresh: Boolean = false) {
        val type = mediaType
        val gate = hideAdultState
        val hasData = feeds[type]?.sections?.isNotEmpty() == true
        val mark = lastLoaded[type]
        if (!forceRefresh && mark != null && mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL && hasData) return
        // Drop overlapping loads so a slower (stale) response cannot overwrite
        // a newer one. Defer instead of losing: a forced reload, or a load for
        // a type other than the one in flight (the selector changed mid-load).
        if (loadingState || refreshingState) {
            if (forceRefresh || inFlightType != type) {
                pendingReload = true
                pendingForce = pendingForce || forceRefresh
            }
            return
        }
        val isRefresh = forceRefresh && hasData
        if (isRefresh) {
            refreshingState = true
        } else {
            loadingState = true
        }
        inFlightType = type
        errors.remove(type)
        scope.launch {
            try {
                when (val result = repository.loadFeed(type, gate, strings)) {
                    is NetworkResult.Success -> {
                        feeds[type] = result.value
                        lastLoaded[type] = timeMark.markNow()
                        cache.save(type, gate, result.value)
                    }
                    is NetworkResult.Failure -> errors[type] = result.error
                }
            } finally {
                loadingState = false
                refreshingState = false
                inFlightType = null
                // Run the deferred request (if any) with the value in effect now.
                if (pendingReload) {
                    val force = pendingForce
                    pendingReload = false
                    pendingForce = false
                    load(forceRefresh = force)
                }
            }
        }
    }

    /** Hydrate the selected type from disk without a spinner (stale-while-revalidate). */
    private fun hydrateFromCache(type: DashboardMediaType) {
        val cached = cache.load(type, hideAdultState) ?: return
        feeds[type] = cached.feed
        val ageMs = Clock.System.now().toEpochMilliseconds() - cached.savedAtMillis
        if (ageMs in 0 until AniListNetworkPolicy.CACHE_TTL.inWholeMilliseconds) {
            // Fresh enough: treat as loaded so the immediate load() is a no-op.
            lastLoaded[type] = timeMark.markNow()
        }
    }

    /** Warm the other type once (respects the in-memory cache; no error surface). */
    fun preloadOther() {
        val other = if (mediaType == DashboardMediaType.ANIME) DashboardMediaType.MANGA else DashboardMediaType.ANIME
        if (preloading || feeds[other]?.sections?.isNotEmpty() == true) return
        val gate = hideAdultState
        preloading = true
        scope.launch {
            try {
                when (val result = repository.loadFeed(other, gate, strings)) {
                    is NetworkResult.Success -> {
                        feeds[other] = result.value
                        lastLoaded[other] = timeMark.markNow()
                        cache.save(other, gate, result.value)
                    }
                    is NetworkResult.Failure -> Unit
                }
            } finally {
                preloading = false
            }
        }
    }
}

/** Pure-logic factory. Loading is explicit (the screen triggers it), so
 *  creating a holder never fires a request. */
internal fun createDashboardStateHolder(
    repository: DashboardRepository,
    cache: DashboardFeedCache,
    scope: CoroutineScope,
    strings: LanguageStrings,
): DashboardStateHolder {
    return DashboardStateHolder(repository, cache, scope, strings)
}
