package com.luum.michi.app.notifications.data

import com.luum.michi.app.core.anilist.dto.NotificationsResponseDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import com.luum.michi.app.notifications.presentation.model.NotificationFilter
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val NotificationsQuery = """
query Notifications(${'$'}page: Int, ${'$'}reset: Boolean, ${'$'}withCount: Boolean = false, ${'$'}types: [NotificationType]) {
  Viewer @include(if: ${'$'}withCount) { unreadNotificationCount }
  Page(page: ${'$'}page) {
    pageInfo { hasNextPage }
    notifications(type_in: ${'$'}types, resetNotificationCount: ${'$'}reset) {
      ... on AiringNotification { id type episode media { id title { userPreferred romaji english native } coverImage { extraLarge large medium color } } createdAt }
      ... on FollowingNotification { id type user { id name avatar { large medium } } createdAt }
      ... on ActivityLikeNotification { id type activityId user { id name avatar { large medium } } createdAt }
      ... on ActivityReplyNotification { id type activityId user { id name avatar { large medium } } createdAt }
      ... on ActivityReplyLikeNotification { id type activityId user { id name avatar { large medium } } createdAt }
      ... on ActivityMentionNotification { id type activityId user { id name avatar { large medium } } createdAt }
      ... on ActivityMessageNotification { id type activityId user { id name avatar { large medium } } createdAt }
      ... on RelatedMediaAdditionNotification { id type media { id title { userPreferred romaji english native } coverImage { extraLarge large medium color } } createdAt }
      ... on MediaDataChangeNotification { id type reason media { id title { userPreferred romaji english native } coverImage { extraLarge large medium color } } createdAt }
      ... on MediaMergeNotification { id type reason media { id title { userPreferred romaji english native } coverImage { extraLarge large medium color } } createdAt }
      ... on MediaDeletionNotification { id type reason deletedMediaTitle createdAt }
      ... on ThreadCommentReplyNotification { id type user { id name avatar { large medium } } createdAt }
      ... on ThreadCommentMentionNotification { id type user { id name avatar { large medium } } createdAt }
    }
  }
}
"""

internal class NotificationsRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : NotificationsRepository {

    override suspend fun loadNotifications(
        page: Int,
        resetCount: Boolean,
        filter: NotificationFilter,
    ): NetworkResult<NotificationsPage> {
        val types = filter.toAniListTypes()
        val variables = buildJsonObject {
            put("page", page)
            put("reset", resetCount)
            put("withCount", resetCount)
            // Omit `types` entirely when unfiltered (ALL). Passing an explicit
            // null to AniList's `type_in` makes the notifications resolver fail;
            // an unprovided nullable variable is treated as an absent argument.
            if (types != null) {
                put("types", buildJsonArray { types.forEach { add(it) } })
            }
        }

        val request = AniListGraphQLRequest(
            query = NotificationsQuery,
            variables = variables,
            operationName = "Notifications",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(NotificationsResponseDto.serializer(), dataJson)
        }.map { response ->
            NotificationsPage(
                notifications = response.page.notifications.mapNotNull { it?.toAppNotification() },
                hasNextPage = response.page.pageInfo.hasNextPage,
                unreadCount = response.viewer?.unreadNotificationCount,
            )
        }
    }
}
