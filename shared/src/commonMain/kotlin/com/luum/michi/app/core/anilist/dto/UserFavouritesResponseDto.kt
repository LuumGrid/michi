package com.luum.michi.app.core.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shape for the user's favourites query (anime, manga, characters,
 * staff, studios — all in one round trip).
 *
 * Sample GraphQL:
 * ```
 * query UserFavourites($userId: Int!) {
 *   User(id: $userId) {
 *     favourites {
 *       anime(perPage: 12) { nodes { id title { ... } coverImage { ... } } }
 *       manga(perPage: 12) { nodes { ... } }
 *       characters(perPage: 12) { nodes { id name { ... } image { ... } } }
 *       staff(perPage: 12) { nodes { id name { ... } image { ... } } }
 *       studios(perPage: 12) { nodes { id name } }
 *     }
 *   }
 * }
 * ```
 */
@Serializable
internal data class UserFavouritesResponseDto(
    @SerialName("User")
    val user: UserFavouritesContainerDto? = null,
)

@Serializable
internal data class UserFavouritesContainerDto(
    val favourites: FavouritesDto? = null,
)

@Serializable
internal data class FavouritesDto(
    val anime: MediaConnectionDto? = null,
    val manga: MediaConnectionDto? = null,
    val characters: CharacterConnectionDto? = null,
    val staff: StaffConnectionDto? = null,
    val studios: StudioConnectionDto? = null,
)

@Serializable
internal data class MediaConnectionDto(
    val nodes: List<MediaDto> = emptyList(),
    val pageInfo: PageInfoDto? = null,
)

@Serializable
internal data class CharacterConnectionDto(
    val nodes: List<CharacterDto> = emptyList(),
    val pageInfo: PageInfoDto? = null,
)

@Serializable
internal data class StaffConnectionDto(
    val nodes: List<StaffDto> = emptyList(),
    val pageInfo: PageInfoDto? = null,
)

@Serializable
internal data class StudioConnectionDto(
    val nodes: List<StudioDto> = emptyList(),
    val pageInfo: PageInfoDto? = null,
)
