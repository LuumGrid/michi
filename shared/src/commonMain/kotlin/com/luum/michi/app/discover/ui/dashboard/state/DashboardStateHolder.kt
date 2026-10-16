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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.TimeSource

internal class DashboardStateHolder(
    private val repository: DashboardRepository,
    private val cache: DashboardFeedCache,
    private val scope: CoroutineScope,
    hideAdult: Boolean,
    // Read once per request, before it launches: one load never mixes languages.
    private val stringsProvider: () -> LanguageStrings,
    // Selector value at creation ("Pestaña al abrir" → DISCOVER sub-choice).
    initialType: DashboardMediaType = DashboardMediaType.ANIME,
) {
    // Selected selector value; loads the other type on switch (cached per type).
    var mediaType: DashboardMediaType by mutableStateOf(initialType)
        private set

    // Snapshot maps so a completed load recomposes the screen.
    private val feeds = mutableStateMapOf<DashboardMediaType, DashboardFeed>()
    private val errors = mutableStateMapOf<DashboardMediaType, NetworkError>()
    // Loading lives per type: an Anime request finishing must not clear (or set)
    // Manga's indicator, which is what kept a pull-to-refresh spinning forever
    // after switching to a type served from the cache.
    private val loading = mutableStateMapOf<DashboardMediaType, Boolean>()
    private val refreshing = mutableStateMapOf<DashboardMediaType, Boolean>()
    private val lastLoaded = mutableMapOf<DashboardMediaType, TimeSource.Monotonic.ValueTimeMark>()

    // One request per type at a time: a select during a preload joins it.
    private val inFlight = mutableMapOf<DashboardMediaType, Job>()
    // Gate each in-flight request was launched with, so a gate change mid-flight
    // can schedule a reload of the new key (the stale response is discarded).
    private val inFlightGate = mutableMapOf<DashboardMediaType, Boolean>()
    private val timeMark = TimeSource.Monotonic

    // 18+ gate, applied from creation so the screen's first sync is a no-op and
    // a cold start with an allowed gate and a fresh cache never hits the network.
    private var hideAdultState: Boolean = hideAdult

    // Bumped by invalidate(): a response from an older generation is dropped
    // (never rewrites memory or disk with the previous language's `meta`).
    // Observable so the screen can key its load effect on it.
    private var generationState by mutableStateOf(0)

    // A load request that arrives while another is in flight would be dropped;
    // remember it (and whether it was forced) so the in-flight response is
    // followed by a reload with the selection/value in effect at that point.
    private var pendingReload = false
    private var pendingForce = false

    /** Cache generation; a change means "reload when the screen is visible". */
    val generation: Int get() = generationState

    val isLoading: Boolean get() = loading[mediaType] == true
    val isRefreshing: Boolean get() = refreshing[mediaType] == true

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

    /**
     * Mirror the 18+ setting. The disk cache is keyed by the gate, so a fresh
     * cache under the new value is valid: drop the in-memory rails (they belong
     * to the old gate) and reload **without forcing**, hydrating the new key.
     * The first sync (equal to the value applied at creation) is a no-op.
     */
    fun setHideAdult(hideAdult: Boolean) {
        if (hideAdultState == hideAdult) return
        hideAdultState = hideAdult
        feeds.clear()
        lastLoaded.clear()
        errors.clear()
        load()
    }

    /**
     * The app language changed: the cached `meta` is localized, so drop disk +
     * memory and bump the generation. Launches nothing — the screen reloads
     * when it is (re)composed, so a change made on another tab or under
     * Settings never fires a background request.
     */
    fun invalidate() {
        generationState += 1
        // Cancel any in-flight request: its response would be discarded by the
        // generation check, and a load() arriving meanwhile would otherwise
        // "join" that doomed request instead of firing a fresh one.
        inFlight.values.forEach { it.cancel() }
        inFlight.clear()
        inFlightGate.clear()
        pendingReload = false
        pendingForce = false
        cache.clear()
        feeds.clear()
        lastLoaded.clear()
        errors.clear()
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
        // firing a second one for the same type+gate. When the in-flight request
        // carries a different gate (or a force was asked for), the response will
        // not satisfy this call, so schedule a reload for after it lands.
        if (inFlight[type]?.isActive == true) {
            if (forceRefresh || inFlightGate[type] != hideAdultState) {
                pendingReload = true
                pendingForce = pendingForce || forceRefresh
            }
            if (hasData) refreshing[type] = true else loading[type] = true
            return
        }
        launchLoad(
            type = type,
            gate = hideAdultState,
            forceRefresh = forceRefresh,
            visible = true,
            strings = stringsProvider(),
        )
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
        launchLoad(
            type = other,
            gate = hideAdultState,
            forceRefresh = false,
            visible = false,
            strings = stringsProvider(),
        )
    }

    private fun launchLoad(
        type: DashboardMediaType,
        gate: Boolean,
        forceRefresh: Boolean,
        visible: Boolean,
        strings: LanguageStrings,
    ) {
        val launchGeneration = generation
        val hasData = feeds[type]?.sections?.isNotEmpty() == true
        val isRefresh = forceRefresh && hasData
        if (visible) {
            if (isRefresh) refreshing[type] = true else loading[type] = true
        }
        errors.remove(type)
        inFlightGate[type] = gate
        inFlight[type] = scope.launch {
            try {
                when (val result = repository.loadFeed(type, gate, strings)) {
                    is NetworkResult.Success -> {
                        // Drop a response from an older generation (language
                        // changed mid-flight): it must not rewrite either cache.
                        if (launchGeneration == generation) {
                            // Discard a response whose gate is no longer in effect.
                            if (gate == hideAdultState) {
                                feeds[type] = result.value
                                lastLoaded[type] = timeMark.markNow()
                            }
                            cache.save(type, gate, result.value)
                        }
                    }
                    is NetworkResult.Failure ->
                        if (visible && launchGeneration == generation && gate == hideAdultState) {
                            errors[type] = result.error
                        }
                }
            } finally {
                // Only clear when this job is still the current one: the finally
                // of a cancelled job must not wipe the state of the load that
                // replaced it.
                if (inFlight[type] === coroutineContext[Job]) {
                    inFlight.remove(type)
                    inFlightGate.remove(type)
                    // Only this type's indicators: another type's request (or a
                    // cached switch) is untouched.
                    loading[type] = false
                    refreshing[type] = false
                    if (pendingReload) {
                        val force = pendingForce
                        pendingReload = false
                        pendingForce = false
                        load(forceRefresh = force)
                    }
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
 *  creating a holder never fires a request. [stringsProvider] is read once per
 *  request so the holder survives a language change (and keeps the selection). */
internal fun createDashboardStateHolder(
    repository: DashboardRepository,
    cache: DashboardFeedCache,
    scope: CoroutineScope,
    hideAdult: Boolean = true,
    initialType: DashboardMediaType = DashboardMediaType.ANIME,
    stringsProvider: () -> LanguageStrings,
): DashboardStateHolder {
    return DashboardStateHolder(repository, cache, scope, hideAdult, stringsProvider, initialType)
}
