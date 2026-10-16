package com.luum.michi.app.core.favourite.domain

import com.luum.michi.app.core.network.domain.NetworkResult

/**
 * What a favourite toggle can target. The single [FavouriteRepository] mutation
 * carries all five as optional variables, so one call handles every case.
 */
internal sealed interface FavouriteTarget {
    val id: Int

    data class Anime(override val id: Int) : FavouriteTarget
    data class Manga(override val id: Int) : FavouriteTarget
    data class Character(override val id: Int) : FavouriteTarget
    data class Staff(override val id: Int) : FavouriteTarget
    data class Studio(override val id: Int) : FavouriteTarget
}

/**
 * One favourite toggle for every entity type, so the four features that can
 * favourite something share a single mutation instead of four copies.
 */
internal interface FavouriteRepository {
    suspend fun toggleFavourite(target: FavouriteTarget): NetworkResult<Unit>
}
