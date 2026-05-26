package com.luum.michi.app.feed.data

import com.luum.michi.app.core.anilist.dto.ActivityDto
import com.luum.michi.app.core.anilist.dto.FeedActivitiesResponseDto
import com.luum.michi.app.feed.presentation.model.FeedActivity

internal fun ActivityDto.toFeedActivity(): FeedActivity? {
    val actId = id ?: return null
    val actType = type ?: return null
    return when (actType) {
        "ANIME_LIST", "MANGA_LIST" -> {
            val actUser = user ?: return null
            val actUserId = actUser.id ?: return null
            val actMedia = media ?: return null
            val actMediaId = actMedia.id ?: return null
            val title = actMedia.title?.userPreferred
                ?: actMedia.title?.romaji
                ?: actMedia.title?.english
                ?: return null
            val statusText = buildString {
                val cap = status?.replaceFirstChar { it.uppercaseChar() } ?: ""
                append(cap)
                if (cap.isNotEmpty()) append(" ")
                val prog = progress
                if (!prog.isNullOrBlank()) append("$prog of ")
            }
            FeedActivity.MediaList(
                id = actId,
                userId = actUserId,
                userName = actUser.name.orEmpty(),
                userAvatarUrl = actUser.avatar?.bestUrl,
                createdAtEpochSeconds = createdAt ?: 0L,
                likeCount = likeCount ?: 0,
                replyCount = replyCount ?: 0,
                siteUrl = siteUrl,
                mediaId = actMediaId,
                mediaTitle = title,
                coverUrl = actMedia.coverImage?.thumbnailUrl,
                isAnime = actMedia.type == "ANIME",
                statusText = statusText,
            )
        }
        "TEXT" -> {
            val actUser = user ?: return null
            val actUserId = actUser.id ?: return null
            FeedActivity.Text(
                id = actId,
                userId = actUserId,
                userName = actUser.name.orEmpty(),
                userAvatarUrl = actUser.avatar?.bestUrl,
                createdAtEpochSeconds = createdAt ?: 0L,
                likeCount = likeCount ?: 0,
                replyCount = replyCount ?: 0,
                siteUrl = siteUrl,
                text = text.orEmpty(),
            )
        }
        "MESSAGE" -> {
            val actMessenger = messenger ?: return null
            val actMessengerId = actMessenger.id ?: return null
            FeedActivity.Message(
                id = actId,
                userId = actMessengerId,
                userName = actMessenger.name.orEmpty(),
                userAvatarUrl = actMessenger.avatar?.bestUrl,
                createdAtEpochSeconds = createdAt ?: 0L,
                likeCount = likeCount ?: 0,
                replyCount = replyCount ?: 0,
                siteUrl = siteUrl,
                messengerName = actMessenger.name.orEmpty(),
                recipientName = recipient?.name.orEmpty(),
                message = message.orEmpty(),
            )
        }
        else -> null
    }
}

internal fun FeedActivitiesResponseDto.toFeedPage(): FeedPage = FeedPage(
    activities = page.activities.mapNotNull { it?.toFeedActivity() },
    hasNextPage = page.pageInfo.hasNextPage,
)
