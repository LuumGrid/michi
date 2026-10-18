package com.luum.michi.app.profile.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.favourite.domain.FavouriteRepository
import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.core.network.domain.AniListNetworkPolicy
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.profile.domain.ProfileData
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.domain.model.ProfileFavoriteKey
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.domain.model.ProfileStats
import com.luum.michi.app.profile.domain.model.itemsFor
import com.luum.michi.app.profile.domain.model.removeFavorite
import com.luum.michi.app.profile.domain.model.restoreFavorite
import com.luum.michi.app.profile.domain.model.without
import com.luum.michi.app.profile.ui.model.toFavouriteTarget
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
    // Tells Root's favourite router an item was removed here, so a detail page
    // still in the stack or cache flips its heart too (no feature import).
    private val onFavouriteChanged: (FavouriteTarget, Boolean) -> Unit = { _, _ -> },
) {
    private var statsState by mutableStateOf(EmptyStats)
    private var favoritesState by mutableStateOf(ProfileFavorites.EMPTY)
    private var loadingState by mutableStateOf(false)
    private var refreshingState by mutableStateOf(false)
    private var errorState by mutableStateOf<NetworkError?>(null)
    /** Non-blocking failure of a removal: the profile stays on screen. */
    private var actionErrorState by mutableStateOf<NetworkError?>(null)
    /** True while anything for [renderedUserId] is painted (disk or network). */
    private var paintedState by mutableStateOf(false)
    /** True once the network confirmed this user's profile this session. Gates
     *  editing: a disk-only (or failed-revalidation) profile must not toggle. */
    private var confirmedState by mutableStateOf(false)
    /** Removals in flight: their item is disabled and filtered from a late load. */
    private var removingKeys by mutableStateOf(emptySet<ProfileFavoriteKey>())
    /** Removals that succeeded but are not yet folded into a network result. */
    private var removedKeys by mutableStateOf(emptySet<ProfileFavoriteKey>())

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
    val actionError: NetworkError? get() = actionErrorState

    /** True once a profile is on screen (from disk or the network), so the
     *  screen can tell "nothing yet" apart from a real all-zero account. */
    val hasContent: Boolean get() = paintedState

    /** Editing the favourites is safe only once the network confirmed the
     *  profile: on disk-only data a removal could re-add what the web removed. */
    val canEdit: Boolean get() = confirmedState

    fun clearActionError() {
        actionErrorState = null
    }

    /** True while [id]'s removal in [category] is in flight (its button is off). */
    fun isRemoving(category: ProfileFavoritesCategory, id: Int): Boolean =
        ProfileFavoriteKey(category, id) in removingKeys

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
            confirmedState = false
            removingKeys = emptySet()
            removedKeys = emptySet()
        }
        // "Has data" keys on what is painted, not on the stats being non-zero:
        // a real account with everything at 0 is still loaded data.
        val isRefresh = forceRefresh && paintedState
        loadingState = false
        refreshingState = false
        if (isRefresh) refreshingState = true else loadingState = true
        errorState = null
        actionErrorState = null
        // Removals already applied when this load started: any that land after
        // must not be repainted by this (older) result.
        val removedAtLoadStart = removedKeys
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
                    val removedSince = removedKeys - removedAtLoadStart
                    statsState = result.value.stats
                    favoritesState = result.value.favorites.without(removingKeys + removedSince)
                    // Folded in: the server truth (minus pending removals) is now
                    // the visible state, so these no longer need filtering.
                    removedKeys = emptySet()
                    renderedUserId = userId
                    paintedState = true
                    confirmedState = true
                    lastLoaded = timeMark.markNow()
                    lastUserId = userId
                    loadedRevision = favouriteRepository.revision.value
                }
                is NetworkResult.Failure -> if (isCurrent()) {
                    errorState = result.error
                }
            }
        }
    }

    /**
     * Removes one favourite: optimistic (it disappears now), one toggle request,
     * rollback on failure, and the disk cache is rewritten on success. No-op
     * while the profile is not network-confirmed or this item is already leaving.
     */
    fun removeFavorite(category: ProfileFavoritesCategory, id: Int) {
        if (!confirmedState) return
        val userId = renderedUserId ?: return
        val key = ProfileFavoriteKey(category, id)
        if (key in removingKeys) return
        val index = favoritesState.itemsFor(category).indexOfFirst { it.id == id }
        if (index < 0) return
        val favorite = favoritesState.itemsFor(category)[index]

        favoritesState = favoritesState.removeFavorite(category, id)
        removingKeys = removingKeys + key
        actionErrorState = null
        val target = category.toFavouriteTarget(id)
        // Optimistic for the detail holders too: a cached/stacked detail flips now.
        onFavouriteChanged(target, false)
        scope.launch {
            when (val result = favouriteRepository.toggleFavourite(target)) {
                is NetworkResult.Success -> {
                    removedKeys = removedKeys + key
                    repository.saveCachedProfile(userId, ProfileData(statsState, favoritesState))
                    loadedRevision = favouriteRepository.revision.value
                }
                is NetworkResult.Failure -> {
                    favoritesState = favoritesState.restoreFavorite(category, index, favorite)
                    actionErrorState = result.error
                    onFavouriteChanged(target, true)
                }
            }
            removingKeys = removingKeys - key
        }
    }

    /**
     * Reads the disk cache once per user, before the network. The file is never
     * fresh ([lastLoaded] stays null), so the fetch always runs; while it does,
     * hydrated data keeps the full-page spinner off (the revalidation is silent).
     * Disk data is not network-confirmed, so editing stays disabled until the
     * revalidation answers.
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
    onFavouriteChanged: (FavouriteTarget, Boolean) -> Unit = { _, _ -> },
): ProfileStateHolder {
    return ProfileStateHolder(repository, favouriteRepository, scope, onFavouriteChanged)
}
