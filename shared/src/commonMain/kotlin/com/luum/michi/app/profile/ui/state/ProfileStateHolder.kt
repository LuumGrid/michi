package com.luum.michi.app.profile.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.favourite.domain.FavouriteRepository
import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.profile.domain.ProfileData
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.domain.model.ProfileStats
import com.luum.michi.app.profile.domain.model.itemsFor
import com.luum.michi.app.profile.domain.model.removeFavorite
import com.luum.michi.app.ui.state.LatestRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

internal val EmptyStats = ProfileStats(
    animeCount = 0,
    mangaCount = 0,
    followingCount = 0,
    followersCount = 0,
)

internal class ProfileStateHolder(
    private val repository: ProfileRepository,
    private val favouriteRepository: FavouriteRepository,
    private val scope: CoroutineScope,
    // Forwards the Viewer's unread count (from the same profile request) to the
    // notifications holder, the single owner of the dot. No copy is kept here.
    private val onUnreadNotificationCount: (Int) -> Unit = {},
) {
    private var statsState by mutableStateOf(EmptyStats)
    private var favoritesState by mutableStateOf(ProfileFavorites.EMPTY)
    private var loadingState by mutableStateOf(false)
    private var refreshingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    /** True while anything for [renderedUserId] is painted (disk or network). */
    private var paintedState by mutableStateOf(false)

    private val timeMark = TimeSource.Monotonic
    private var lastLoaded: TimeSource.Monotonic.ValueTimeMark? = null
    /** Last successful network load: the TTL shortcut keys on it. */
    private var lastUserId: Int? = null
    /** User whose data is on screen right now (painted from disk or network). */
    private var renderedUserId: Int? = null
    /** User whose disk cache was already read, so a cold load reads it once. */
    private var hydratedUserId: Int? = null
    /** Favourite revision this profile was loaded at (see [FavouriteRepository.revision]). */
    private var loadedRevision: Int = 0

    /** Last user a load was started for: [refresh] retries this one even if it
     *  failed (a failed switch must not make refresh reload the previous user). */
    private var requestedUserId: Int? = null
    private val loadRequest = LatestRequest()

    val stats: ProfileStats get() = statsState
    val favorites: ProfileFavorites get() = favoritesState
    val isLoading: Boolean get() = loadingState
    val isRefreshing: Boolean get() = refreshingState
    val error: NetworkError? get() = errorState

    /** True once a profile is on screen (from disk or the network), so the
     *  screen can tell "nothing yet" apart from a real all-zero account. */
    val hasContent: Boolean get() = paintedState

    fun load(userId: Int, forceRefresh: Boolean = false) {
        val mark = lastLoaded
        // A favourite toggled elsewhere makes the loaded profile stale: reload
        // on the next open even inside the TTL (the signal adds no request).
        val favouritesStale = favouriteRepository.revision.value != loadedRevision
        if (!forceRefresh && !favouritesStale && hasDataFor(userId) && mark != null
            && mark.elapsedNow() < AniListNetworkPolicy.CACHE_TTL
        ) return
        requestedUserId = userId
        // Switching user must never show the previous user's data while the new
        // one loads; a same-user reload keeps it until the response replaces it.
        if (renderedUserId != userId) {
            statsState = EmptyStats
            favoritesState = ProfileFavorites.EMPTY
            renderedUserId = null
            paintedState = false
        }
        // "Has data" keys on what is painted, not on the stats being non-zero:
        // a real account with everything at 0 is still loaded data.
        val isRefresh = forceRefresh && paintedState
        loadingState = false
        refreshingState = false
        if (isRefresh) refreshingState = true else loadingState = true
        errorState = null
        // Newest load wins: a slower (stale) response cannot overwrite newer
        // data, and only the current request writes state and owns the spinners.
        loadRequest.launch(
            scope = scope,
            onSettled = {
                loadingState = false
                refreshingState = false
            },
        ) { isCurrent ->
            hydrateIfNeeded(userId, isCurrent)
            when (val result = repository.loadProfile(userId)) {
                is NetworkResult.Success -> if (isCurrent()) {
                    statsState = result.value.stats
                    favoritesState = result.value.favorites
                    renderedUserId = userId
                    paintedState = true
                    lastLoaded = timeMark.markNow()
                    lastUserId = userId
                    loadedRevision = favouriteRepository.revision.value
                    // A null (cache / older shape) never reaches the dot, so it
                    // cannot blank a live count.
                    result.value.unreadNotificationCount?.let(onUnreadNotificationCount)
                }
                is NetworkResult.Failure -> if (isCurrent()) {
                    errorState = result.error
                }
            }
        }
    }

    /**
     * Mirrors a removal made from a "View all" grid: drops it from the in-memory
     * favourites and rewrites the disk cache, with no request. The profile's
     * revision is advanced too, so its own removal never marks it stale. A no-op
     * on the data (but still not stale) when the item was not in the rails.
     */
    fun removeFavoriteLocally(category: ProfileFavoritesCategory, id: Int) {
        loadedRevision = favouriteRepository.revision.value
        val userId = renderedUserId ?: return
        if (favoritesState.itemsFor(category).none { it.id == id }) return
        favoritesState = favoritesState.removeFavorite(category, id)
        scope.launch { repository.saveCachedProfile(userId, ProfileData(statsState, favoritesState)) }
    }

    /**
     * Reads the disk cache once per user, before the network. The file is never
     * fresh ([lastLoaded] stays null), so the fetch always runs; while it does,
     * hydrated data keeps the full-page spinner off (the revalidation is silent).
     */
    private suspend fun hydrateIfNeeded(userId: Int, isCurrent: () -> Boolean) {
        if (hydratedUserId == userId) return
        val cached = repository.loadCachedProfile(userId)
        if (!isCurrent()) return
        hydratedUserId = userId
        if (cached == null) return
        statsState = cached.stats
        favoritesState = cached.favorites
        renderedUserId = userId
        paintedState = true
        loadingState = false
    }

    fun refresh() {
        requestedUserId?.let { load(it, forceRefresh = true) }
    }

    /** True once a network load for [userId] succeeded (even with all-zero stats). */
    private fun hasDataFor(userId: Int): Boolean = lastUserId == userId && lastLoaded != null
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createProfileStateHolder(
    repository: ProfileRepository,
    favouriteRepository: FavouriteRepository,
    scope: CoroutineScope,
    onUnreadNotificationCount: (Int) -> Unit = {},
): ProfileStateHolder {
    return ProfileStateHolder(
        repository = repository,
        favouriteRepository = favouriteRepository,
        scope = scope,
        onUnreadNotificationCount = onUnreadNotificationCount,
    )
}
