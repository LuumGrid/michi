package com.luum.michi.app.feed.data

import com.luum.michi.app.core.anilist.dto.FeedActivitiesResponseDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import com.luum.michi.app.feed.presentation.model.FeedActivity
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private const val ActivitiesQuery = """
query Activities(${'$'}page: Int, ${'$'}isFollowing: Boolean, ${'$'}hasRepliesOrText: Boolean, ${'$'}typeIn: [ActivityType], ${'$'}userIdNot: Int) {
  Page(page: ${'$'}page) {
    pageInfo { hasNextPage }
    activities(isFollowing: ${'$'}isFollowing, hasRepliesOrTypeText: ${'$'}hasRepliesOrText, type_in: ${'$'}typeIn, userId_not: ${'$'}userIdNot, sort: [PINNED, ID_DESC]) {
      ... on ListActivity { id type createdAt likeCount replyCount isLiked siteUrl status progress user { id name avatar { large medium } } media { id type format title { userPreferred romaji english } coverImage { large medium color } } }
      ... on TextActivity { id type createdAt likeCount replyCount isLiked siteUrl text user { id name avatar { large medium } } }
      ... on MessageActivity { id type createdAt likeCount replyCount isLiked siteUrl message messenger { id name avatar { large medium } } recipient { id name avatar { large medium } } }
    }
  }
}
"""

internal class FeedRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : FeedRepository {

    override suspend fun loadFeed(filter: FeedFilter, activityFilter: FeedActivityFilter, page: Int, viewerId: Int): NetworkResult<FeedPage> {
        val typeList = buildList {
            if (activityFilter.statuses) add("TEXT")
            if (activityFilter.animationProgress) add("ANIME_LIST")
            if (activityFilter.readingProgress) add("MANGA_LIST")
            if (activityFilter.messages) add("MESSAGE")
        }

        val variables = buildJsonObject {
            put("page", page)
            when (filter) {
                FeedFilter.FOLLOWING -> put("isFollowing", true)
                FeedFilter.GLOBAL -> {
                    put("isFollowing", false)
                    put("hasRepliesOrText", true)
                    if (!activityFilter.myActivities) put("userIdNot", viewerId)
                }
            }
            if (typeList.isNotEmpty()) {
                putJsonArray("typeIn") {
                    typeList.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                }
            }
        }

        val request = AniListGraphQLRequest(
            query = ActivitiesQuery,
            variables = variables,
            operationName = "Activities",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromString(FeedActivitiesResponseDto.serializer(), dataJson)
        }.map { response -> response.toFeedPage() }
    }
}
