package com.luum.michi.app.profile.repository

import com.luum.michi.app.core.media.repository.dto.MediaDto
import com.luum.michi.app.core.network.repository.dto.PageInfoDto
import com.luum.michi.app.core.person.repository.dto.PersonImageDto
import com.luum.michi.app.core.person.repository.dto.PersonNameDto
import com.luum.michi.app.core.person.repository.dto.StudioDto
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
 *       characters(perPage: 12) { nodes { id name { ... } image { ... } media { ... } } }
 *       staff(perPage: 12) { nodes { id name { ... } image { ... } primaryOccupations } }
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
    val nodes: List<FavoriteCharacterDto> = emptyList(),
    val pageInfo: PageInfoDto? = null,
)

@Serializable
internal data class StaffConnectionDto(
    val nodes: List<FavoriteStaffDto> = emptyList(),
    val pageInfo: PageInfoDto? = null,
)

/** A favourited character plus the one media the profile uses for its second
 *  line (the character's most popular anime). */
@Serializable
internal data class FavoriteCharacterDto(
    val id: Int,
    val name: PersonNameDto? = null,
    val image: PersonImageDto? = null,
    val media: FavoriteCharacterMediaDto? = null,
)

@Serializable
internal data class FavoriteCharacterMediaDto(
    val nodes: List<MediaDto> = emptyList(),
)

/** A favourited staff member plus the occupations the profile uses for its
 *  second line. */
@Serializable
internal data class FavoriteStaffDto(
    val id: Int,
    val name: PersonNameDto? = null,
    val image: PersonImageDto? = null,
    val primaryOccupations: List<String?>? = null,
)

@Serializable
internal data class StudioConnectionDto(
    val nodes: List<StudioDto> = emptyList(),
    val pageInfo: PageInfoDto? = null,
)
