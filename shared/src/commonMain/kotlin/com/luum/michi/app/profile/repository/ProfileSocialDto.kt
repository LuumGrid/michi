package com.luum.michi.app.profile.repository

import com.luum.michi.app.core.media.repository.dto.MediaCoverImageDto
import com.luum.michi.app.core.media.repository.dto.MediaTitleDto
import com.luum.michi.app.core.network.repository.dto.PageInfoDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Responses of the profile's social lists (following/followers and reviews).
 * Both are public AniList connections paged with `Page`, so they are cached in
 * memory only (never on disk).
 */
@Serializable
internal data class FollowsListResponseDto(
    @SerialName("Page")
    val page: FollowsListPageDto? = null,
)

@Serializable
internal data class FollowsListPageDto(
    val pageInfo: PageInfoDto? = null,
    val following: List<ProfileUserDto?>? = null,
    val followers: List<ProfileUserDto?>? = null,
)

@Serializable
internal data class ProfileUserDto(
    val id: Int = 0,
    val name: String = "",
    val avatar: ProfileAvatarDto? = null,
)

/** AniList user avatars only ship `large`/`medium` (no extraLarge). */
@Serializable
internal data class ProfileAvatarDto(
    val large: String? = null,
    val medium: String? = null,
)

@Serializable
internal data class ReviewsListResponseDto(
    @SerialName("Page")
    val page: ReviewsListPageDto? = null,
)

@Serializable
internal data class ReviewsListPageDto(
    val pageInfo: PageInfoDto? = null,
    val reviews: List<ProfileReviewDto?> = emptyList(),
)

@Serializable
internal data class ProfileReviewDto(
    val id: Int = 0,
    val summary: String? = null,
    val score: Int = 0,
    val media: ReviewMediaDto? = null,
)

@Serializable
internal data class ReviewMediaDto(
    val id: Int = 0,
    val title: MediaTitleDto? = null,
    val coverImage: MediaCoverImageDto? = null,
)
