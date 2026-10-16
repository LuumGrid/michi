package com.luum.michi.app.profile.repository

import com.luum.michi.app.core.network.repository.dto.PageInfoDto
import kotlinx.serialization.Serializable

/**
 * User statistics blocks (anime/manga counts, score/format/status/genre
 * distributions) plus the followers/following count page, as decoded by the
 * merged profile query.
 */
@Serializable
internal data class UserStatisticTypesDto(
    val anime: UserStatisticsDto? = null,
    val manga: UserStatisticsDto? = null,
)

@Serializable
internal data class UserStatisticsDto(
    val count: Int = 0,
    val meanScore: Double = 0.0,
    val standardDeviation: Double = 0.0,
    val minutesWatched: Int? = null,
    val episodesWatched: Int? = null,
    val chaptersRead: Int? = null,
    val volumesRead: Int? = null,
    val scores: List<UserStatisticScoreDto> = emptyList(),
    val formats: List<UserStatisticFormatDto> = emptyList(),
    val statuses: List<UserStatisticStatusDto> = emptyList(),
    val genres: List<UserStatisticGenreDto> = emptyList(),
)

@Serializable
internal data class UserStatisticScoreDto(
    val score: Int = 0,
    val count: Int = 0,
    val meanScore: Double = 0.0,
)

@Serializable
internal data class UserStatisticFormatDto(
    val format: String? = null,
    val count: Int = 0,
)

@Serializable
internal data class UserStatisticStatusDto(
    val status: String? = null,
    val count: Int = 0,
)

@Serializable
internal data class UserStatisticGenreDto(
    val genre: String? = null,
    val count: Int = 0,
    val meanScore: Double = 0.0,
    val minutesWatched: Int = 0,
)

@Serializable
internal data class FollowsPageDto(
    val pageInfo: PageInfoDto? = null,
)
