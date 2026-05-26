package com.luum.michi.app.core.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FeedActivitiesResponseDto(
    @SerialName("Page") val page: FeedPageDto = FeedPageDto(),
)

@Serializable
internal data class FeedPageDto(
    val pageInfo: FeedPageInfoDto = FeedPageInfoDto(),
    val activities: List<ActivityDto?> = emptyList(),
)

@Serializable
internal data class FeedPageInfoDto(
    val hasNextPage: Boolean = false,
)

@Serializable
internal data class ActivityDto(
    val id: Int? = null,
    val type: String? = null,
    val createdAt: Long? = null,
    val replyCount: Int? = null,
    val likeCount: Int? = null,
    val isLiked: Boolean? = null,
    val siteUrl: String? = null,
    val status: String? = null,
    val progress: String? = null,
    val media: ActivityMediaDto? = null,
    val user: ActivityUserDto? = null,
    val text: String? = null,
    val message: String? = null,
    val messenger: ActivityUserDto? = null,
    val recipient: ActivityUserDto? = null,
)

@Serializable
internal data class ActivityUserDto(
    val id: Int? = null,
    val name: String? = null,
    val avatar: ActivityAvatarDto? = null,
)

@Serializable
internal data class ActivityAvatarDto(
    val large: String? = null,
    val medium: String? = null,
) {
    val bestUrl: String? get() = large ?: medium
}

@Serializable
internal data class ActivityMediaDto(
    val id: Int? = null,
    val type: String? = null,
    val title: MediaTitleDto? = null,
    val coverImage: MediaCoverImageDto? = null,
    val format: String? = null,
)
