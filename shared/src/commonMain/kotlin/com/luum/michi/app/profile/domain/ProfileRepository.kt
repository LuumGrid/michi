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
    suspend fun loadProfile(userId: Int): NetworkResult<ProfileData>

    /** Fetches one page of a single favourites category for the "see more" grid. */
    suspend fun loadFavoritesPage(
        userId: Int,
        category: ProfileFavoritesCategory,
        page: Int,
    ): NetworkResult<Page<ProfileFavorite>>
}
