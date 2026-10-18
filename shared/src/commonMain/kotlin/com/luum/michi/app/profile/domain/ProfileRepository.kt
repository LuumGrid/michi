package com.luum.michi.app.profile.domain

import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileStats
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.domain.NetworkResult

/** Combined result of a single profile data fetch. */
internal data class ProfileData(
    val stats: ProfileStats,
    val favorites: ProfileFavorites,
)

/**
 * Profile data in a single HTTP round-trip: statistics + favourites together,
 * plus one paged favourites category at a time for the "see more" grid.
 */
internal interface ProfileRepository {
    /**
     * Fetches the profile (statistics + favourites) in one round-trip. A
     * successful fetch is persisted so a later cold start can paint it before
     * the network.
     */
    suspend fun loadProfile(userId: Int): NetworkResult<ProfileData>

    /**
     * The last persisted profile for [userId], or null when nothing valid is
     * stored. Never counts as a fresh load: the caller still revalidates against
     * the network.
     */
    suspend fun loadCachedProfile(userId: Int): ProfileData?

    /**
     * Rewrites the persisted profile for [userId] with [data] (the in-memory
     * state after a favourite removal), so a cold start never repaints an
     * already-removed favourite.
     */
    suspend fun saveCachedProfile(userId: Int, data: ProfileData)

    /** Fetches one page of a single favourites category for the "see more" grid. */
    suspend fun loadFavoritesPage(
        userId: Int,
        category: ProfileFavoritesCategory,
        page: Int,
    ): NetworkResult<Page<ProfileFavorite>>
}
