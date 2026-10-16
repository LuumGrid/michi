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
 * One favourite toggle for every entity type. The only consumer today is
 * mediaDetail — directly for character/staff/studio and through
 * `MediaListEntryRepository` for anime/manga — and profile will use it to
 * unfavourite. One mutation instead of a copy per feature.
 */
internal interface FavouriteRepository {
    suspend fun toggleFavourite(target: FavouriteTarget): NetworkResult<Unit>
}
