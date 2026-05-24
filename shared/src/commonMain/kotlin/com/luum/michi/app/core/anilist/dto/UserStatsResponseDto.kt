package com.luum.michi.app.core.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shape for a combined "account stats" query that pulls:
 *   - User.statistics.anime.count
 *   - User.statistics.manga.count
 *   - followers Page total
 *   - following Page total
 * in a single HTTP request.
 *
 * Sample GraphQL:
 * ```
 * query UserStats($userId: Int!) {
 *   User(id: $userId) { statistics { anime { count } manga { count } } }
 *   followers: Page(perPage: 1) { pageInfo { total } followers(userId: $userId) { id } }
 *   following: Page(perPage: 1) { pageInfo { total } following(userId: $userId) { id } }
 * }
 * ```
 */
@Serializable
internal data class UserStatsResponseDto(
    @SerialName("User")
    val user: UserStatsContainerDto? = null,
    val followers: FollowsPageDto? = null,
    val following: FollowsPageDto? = null,
)

@Serializable
internal data class UserStatsContainerDto(
    val statistics: UserStatisticTypesDto? = null,
)

@Serializable
internal data class UserStatisticTypesDto(
    val anime: UserStatisticsDto? = null,
    val manga: UserStatisticsDto? = null,
)

@Serializable
internal data class UserStatisticsDto(
    val count: Int = 0,
    val meanScore: Double = 0.0,
    val minutesWatched: Int? = null,
    val chaptersRead: Int? = null,
    val volumesRead: Int? = null,
)

@Serializable
internal data class FollowsPageDto(
    val pageInfo: PageInfoDto? = null,
)

@Serializable
internal data class PageInfoDto(
    val total: Int = 0,
    val perPage: Int = 0,
    val currentPage: Int = 0,
    val lastPage: Int = 0,
    val hasNextPage: Boolean = false,
)
