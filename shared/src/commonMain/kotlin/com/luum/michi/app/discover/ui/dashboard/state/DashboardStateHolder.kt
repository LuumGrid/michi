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
import kotlinx.coroutines.Job
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

    // One request per type at a time: a select during a preload joins it.
    private val inFlight = mutableMapOf<DashboardMediaType, Job>()
    private val timeMark = TimeSource.Monotonic

    private var loadingState: Boolean by mutableStateOf(false)
    private var refreshingState: Boolean by mutableStateOf(false)

    // 18+ gate, owned here so a change can force a reload. Hidden by default
    // (matches the settings default); `false` lets the server return both.
    private var hideAdultState: Boolean = true

    // A load request that arrives while another is in flight would be dropped;
    // remember it (and whether it was forced) so the in-flight response is
    // followed by a reload with the selection/value in effect at that point.
    private var pendingReload = false
    private var pendingForce = false

    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState

    /** Error of the selected type only: an Anime failure never shows on Manga. */
    val error: NetworkError? get() = errors[mediaType]

    /** Rails for the current [mediaType]; empty until its first load. */
    val sections: Map<DashboardSectionKind, List<MediaItem>>
        get() = feeds[mediaType]?.sections ?: emptyMap()

    /** Selector: switching to an unloaded type hydrates from disk then revalidates. */
    fun selectType(type: DashboardMediaType) {
        if (mediaType == type) return
        mediaType = type
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

    /** The app language changed: the cached `meta` is localized, so drop it all. */
    fun clearCacheAndReload() {
        cache.clear()
        feeds.clear()
        lastLoaded.clear()
        errors.clear()
        load(forceRefresh = true)
    }

    /** Load the current type's rails, skipping the network within the TTL. */
    fun load(forceRefresh: Boolean = false) {
        val type = mediaType
        // Cold start (or a fresh holder): hydrate from disk before deciding on
        // the network, so a cached rail never flashes a spinner.
        if (feeds[type] == null) hydrateFromCache(type)
        val hasData = feeds[type]?.sections?.isNotEmpty() == true
        val mark = lastLoaded[type]
        if (!forceRefresh && mark != null && mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL && hasData) return
        // Join an in-flight request for this type (e.g. a preload) instead of
        // firing a second one for the same type+gate.
        if (inFlight[type]?.isActive == true) {
            if (forceRefresh) {
                pendingReload = true
                pendingForce = true
            } else if (hasData) {
                refreshingState = true
            } else {
                loadingState = true
            }
            return
        }
        launchLoad(type = type, gate = hideAdultState, forceRefresh = forceRefresh, visible = true)
    }

    /** Warm the other type once: hydrate first, skip the network when fresh. */
    fun preloadOther() {
        val other = if (mediaType == DashboardMediaType.ANIME) DashboardMediaType.MANGA else DashboardMediaType.ANIME
        if (inFlight[other]?.isActive == true) return
        if (feeds[other] == null) hydrateFromCache(other)
        val mark = lastLoaded[other]
        if (mark != null && mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL &&
            feeds[other]?.sections?.isNotEmpty() == true
        ) {
            return
        }
        launchLoad(type = other, gate = hideAdultState, forceRefresh = false, visible = false)
    }

    private fun launchLoad(
        type: DashboardMediaType,
        gate: Boolean,
        forceRefresh: Boolean,
        visible: Boolean,
    ) {
        val hasData = feeds[type]?.sections?.isNotEmpty() == true
        val isRefresh = forceRefresh && hasData
        if (visible) {
            if (isRefresh) refreshingState = true else loadingState = true
        }
        errors.remove(type)
        inFlight[type] = scope.launch {
            try {
                when (val result = repository.loadFeed(type, gate, strings)) {
                    is NetworkResult.Success -> {
                        // Discard a response whose gate is no longer in effect.
                        if (gate == hideAdultState) {
                            feeds[type] = result.value
                            lastLoaded[type] = timeMark.markNow()
                        }
                        cache.save(type, gate, result.value)
                    }
                    is NetworkResult.Failure ->
                        if (visible && gate == hideAdultState) errors[type] = result.error
                }
            } finally {
                inFlight.remove(type)
                if (type == mediaType) {
                    loadingState = false
                    refreshingState = false
                }
                if (pendingReload) {
                    val force = pendingForce
                    pendingReload = false
                    pendingForce = false
                    load(forceRefresh = force)
                }
            }
        }
    }

    /** Hydrate [type] from disk (no spinner); a fresh cache counts as loaded. */
    private fun hydrateFromCache(type: DashboardMediaType) {
        val cached = cache.load(type, hideAdultState) ?: return
        feeds[type] = cached.feed
        val ageMs = Clock.System.now().toEpochMilliseconds() - cached.savedAtMillis
        if (ageMs in 0 until AniListNetworkPolicy.CACHE_TTL.inWholeMilliseconds) {
            lastLoaded[type] = timeMark.markNow()
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
