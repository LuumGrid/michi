package com.luum.michi.app.account.domain

import com.luum.michi.app.account.domain.model.AccountFavorites
import com.luum.michi.app.account.domain.model.AccountFavoritesCategory
import com.luum.michi.app.account.domain.model.AccountStats
import com.luum.michi.app.core.domain.network.NetworkResult

/** Combined result of a single account data fetch. */
internal data class AccountData(
    val stats: AccountStats,
    val favorites: AccountFavorites,
)

/** One page of a single favourites category for the paginated "see more" grid. */
internal data class AccountFavoritesPage(
    val mediaItems: List<com.luum.michi.app.account.domain.model.AccountFavoriteMedia> = emptyList(),
    val personItems: List<com.luum.michi.app.account.domain.model.AccountFavoritePerson> = emptyList(),
    val studioItems: List<com.luum.michi.app.account.domain.model.AccountFavoriteStudio> = emptyList(),
    val hasNextPage: Boolean = false,
)

/**
 * Account data in a single HTTP round-trip: statistics + favourites together,
 * plus one paged favourites category at a time for the "see more" grid.
 */
internal interface AccountRepository {
    suspend fun loadAccount(userId: Int): NetworkResult<AccountData>

    /** Fetches one page of a single favourites category for the "see more" grid. */
    suspend fun loadFavoritesPage(
        userId: Int,
        category: AccountFavoritesCategory,
        page: Int,
    ): NetworkResult<AccountFavoritesPage>
}
