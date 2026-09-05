package com.luum.michi.app.account.domain

import com.luum.michi.app.account.domain.model.AccountFavorites
import com.luum.michi.app.account.domain.model.AccountFavoritesCategory
import com.luum.michi.app.account.domain.model.AccountStats
import com.luum.michi.app.core.network.NetworkResult

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
 * Single-query alternative to the two separate [AccountStatsRepository] and
 * [AccountFavoritesRepository] calls. Fetches statistics + favourites in one
 * HTTP round-trip.
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
