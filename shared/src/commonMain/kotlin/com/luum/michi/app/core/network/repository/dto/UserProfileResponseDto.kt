package com.luum.michi.app.core.network.repository.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Combined response DTO for the merged profile query that fetches user stats
 * (statistics + followers/following counts) AND favourites in a single request.
 *
 * GraphQL:
 * ```
 * query UserProfile($userId: Int!) {
 *   User(id: $userId) {
 *     statistics { anime { count meanScore minutesWatched } manga { count meanScore chaptersRead volumesRead } }
 *     favourites {
 *       anime(perPage: 12) { nodes { id title { ... } coverImage { ... } } }
 *       manga(perPage: 12) { nodes { ... } }
 *       characters(perPage: 12) { nodes { id name { ... } image { ... } } }
 *       staff(perPage: 12) { nodes { id name { ... } image { ... } } }
 *       studios(perPage: 12) { nodes { id name media { nodes { status coverImage { ... } } } } }
 *     }
 *   }
 *   followers: Page(perPage: 1) { pageInfo { total } followers(userId: $userId) { id } }
 *   following: Page(perPage: 1) { pageInfo { total } following(userId: $userId) { id } }
 * }
 * ```
 */
@Serializable
internal data class UserProfileResponseDto(
    @SerialName("User")
    val user: UserProfileContainerDto? = null,
    val followers: FollowsPageDto? = null,
    val following: FollowsPageDto? = null,
)

@Serializable
internal data class UserProfileContainerDto(
    val statistics: UserStatisticTypesDto? = null,
    val favourites: FavouritesDto? = null,
)
