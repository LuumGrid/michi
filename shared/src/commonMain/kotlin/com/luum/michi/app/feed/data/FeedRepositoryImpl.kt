package com.luum.michi.app.feed.data

import com.luum.michi.app.core.anilist.dto.FeedActivitiesResponseDto
import com.luum.michi.app.core.anilist.dto.MediaTitleDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import com.luum.michi.app.feed.presentation.model.FeedActivity
import com.luum.michi.app.feed.presentation.model.FeedReview
import com.luum.michi.app.feed.presentation.model.FeedReviewsPage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private const val GlobalReviewsPerPage = 25

private const val GlobalReviewsQuery = """
query GlobalReviews(${'$'}page: Int!, ${'$'}perPage: Int!) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    pageInfo { hasNextPage }
    reviews(sort: [CREATED_AT_DESC]) {
      id
      summary
      rating
      ratingAmount
      user { id name avatar { large } }
      media { id title { romaji english native userPreferred } }
    }
  }
}
"""

@Serializable
private data class GlobalReviewsResponseDto(
    @kotlinx.serialization.SerialName("Page") val page: GlobalReviewsPageDto = GlobalReviewsPageDto(),
)

@Serializable
private data class GlobalReviewsPageDto(
    val pageInfo: GlobalReviewsPageInfoDto = GlobalReviewsPageInfoDto(),
    val reviews: List<GlobalReviewNodeDto> = emptyList(),
)

@Serializable
private data class GlobalReviewsPageInfoDto(
    val hasNextPage: Boolean = false,
)

@Serializable
private data class GlobalReviewNodeDto(
    val id: Int,
    val summary: String? = null,
    val rating: Int = 0,
    val ratingAmount: Int = 0,
    val user: GlobalReviewUserDto? = null,
    val media: GlobalReviewMediaDto? = null,
)

@Serializable
private data class GlobalReviewUserDto(
    val id: Int,
    val name: String,
    val avatar: GlobalReviewAvatarDto? = null,
)

@Serializable
private data class GlobalReviewAvatarDto(
    val large: String? = null,
)

@Serializable
private data class GlobalReviewMediaDto(
    val id: Int,
    val title: MediaTitleDto? = null,
)

private fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

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
            AniListJson.decodeFromJsonElement(FeedActivitiesResponseDto.serializer(), dataJson)
        }.map { response -> response.toFeedPage() }
    }

    override suspend fun loadReviews(page: Int): NetworkResult<FeedReviewsPage> {
        val variables = buildJsonObject {
            put("page", page)
            put("perPage", GlobalReviewsPerPage)
        }

        val request = AniListGraphQLRequest(
            query = GlobalReviewsQuery,
            variables = variables,
            operationName = "GlobalReviews",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(GlobalReviewsResponseDto.serializer(), dataJson)
        }.map { response ->
            FeedReviewsPage(
                reviews = response.page.reviews.map { node ->
                    FeedReview(
                        id = node.id,
                        summary = node.summary.orEmpty(),
                        rating = "${node.rating}/${node.ratingAmount}",
                        reviewerName = node.user?.name.orEmpty(),
                        reviewerImageUrl = node.user?.avatar?.large,
                        mediaId = node.media?.id ?: 0,
                        mediaTitle = node.media?.title.bestTitle(),
                    )
                },
                hasNextPage = response.page.pageInfo.hasNextPage,
            )
        }
    }
}
