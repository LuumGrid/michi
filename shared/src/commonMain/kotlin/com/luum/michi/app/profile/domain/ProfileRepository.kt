package com.luum.michi.app.profile.domain

import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.domain.model.ProfileStats
import com.luum.michi.app.core.network.domain.NetworkResult

/** Combined result of a single profile data fetch. */
internal data class ProfileData(
    val stats: ProfileStats,
    val favorites: ProfileFavorites,
)

/** One page of a single favourites category for the paginated "see more" grid. */
internal data class ProfileFavoritesPage(
    val mediaItems: List<com.luum.michi.app.profile.domain.model.ProfileFavoriteMedia> = emptyList(),
    val personItems: List<com.luum.michi.app.profile.domain.model.ProfileFavoritePerson> = emptyList(),
    val studioItems: List<com.luum.michi.app.profile.domain.model.ProfileFavoriteStudio> = emptyList(),
    val hasNextPage: Boolean = false,
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
    ): NetworkResult<ProfileFavoritesPage>
}
