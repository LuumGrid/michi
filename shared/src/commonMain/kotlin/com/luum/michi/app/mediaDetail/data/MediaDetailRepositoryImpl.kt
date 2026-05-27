package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.anilist.dto.MediaCharacterConnectionDto
import com.luum.michi.app.core.anilist.dto.MediaDetailResponseDto
import com.luum.michi.app.core.anilist.dto.MediaStaffConnectionDto
import com.luum.michi.app.core.anilist.dto.MediaPageInfoDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import com.luum.michi.app.core.media.millisToCalendarParts
import com.luum.michi.app.mediaDetail.presentation.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement

private const val CharactersPerPage = 12
private const val StaffPerPage = 12

private const val MediaDetailQuery = """
query MediaDetail(${'$'}id: Int!, ${'$'}voiceLanguage: StaffLanguage) {
  Media(id: ${'$'}id) {
    id
    type
    title { romaji english native userPreferred }
    description(asHtml: true)
    format
    status
    episodes
    chapters
    volumes
    duration
    genres
    averageScore
    meanScore
    popularity
    favourites
    coverImage { extraLarge large medium color }
    bannerImage
    source
    season
    seasonYear
    startDate { year month day }
    endDate { year month day }
    studios(isMain: true) { nodes { id name } }
    nextAiringEpisode { episode airingAt timeUntilAiring }
    countryOfOrigin
    isAdult
    isFavourite
    mediaListEntry { id status progress progressVolumes score notes repeat priority private hiddenFromStatusLists startedAt { year month day } completedAt { year month day } }
    relations {
      edges {
        relationType(version: 2)
        node {
          id
          type
          format
          title { romaji english native userPreferred }
          coverImage { extraLarge large medium color }
          averageScore
          favourites
          seasonYear
          startDate { year month day }
          mediaListEntry { status }
        }
      }
    }
    stats {
      scoreDistribution { score amount }
      statusDistribution { status amount }
    }
    characters(page: 1, perPage: $CharactersPerPage, sort: [ROLE, RELEVANCE]) {
      pageInfo { hasNextPage currentPage }
      edges {
        id
        role
        node { id name { full native userPreferred } image { large medium } }
        voiceActors(language: ${'$'}voiceLanguage, sort: [RELEVANCE]) {
          id
          name { full native userPreferred }
          image { large medium }
          languageV2
        }
      }
    }
    staff(page: 1, perPage: $StaffPerPage, sort: [RELEVANCE]) {
      pageInfo { hasNextPage currentPage }
      edges {
        id
        role
        node { id name { full native userPreferred } image { large medium } }
      }
    }
  }
}
"""

private const val MediaCharactersPageQuery = """
query MediaCharactersPage(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}voiceLanguage: StaffLanguage) {
  Media(id: ${'$'}id) {
    id
    characters(page: ${'$'}page, perPage: $CharactersPerPage, sort: [ROLE, RELEVANCE]) {
      pageInfo { hasNextPage currentPage }
      edges {
        id
        role
        node { id name { full native userPreferred } image { large medium } }
        voiceActors(language: ${'$'}voiceLanguage, sort: [RELEVANCE]) {
          id
          name { full native userPreferred }
          image { large medium }
          languageV2
        }
      }
    }
  }
}
"""

private const val MediaStaffPageQuery = """
query MediaStaffPage(${'$'}id: Int!, ${'$'}page: Int!) {
  Media(id: ${'$'}id) {
    id
    staff(page: ${'$'}page, perPage: $StaffPerPage, sort: [RELEVANCE]) {
      pageInfo { hasNextPage currentPage }
      edges {
        id
        role
        node { id name { full native userPreferred } image { large medium } }
      }
    }
  }
}
"""

private const val MediaReviewsPageQuery = """
query MediaReviewsPage(${'$'}mediaId: Int!, ${'$'}page: Int!) {
  Media(id: ${'$'}mediaId) {
    id
    reviews(page: ${'$'}page, perPage: 10, sort: [ID_DESC]) {
      pageInfo { hasNextPage currentPage }
      nodes {
        id
        summary
        rating
        ratingAmount
        body(asHtml: false)
        user {
          id
          name
          avatar { large }
        }
      }
    }
  }
}
"""

private const val MediaThreadsPageQuery = """
query MediaThreadsPage(${'$'}page: Int!, ${'$'}mediaCategoryId: Int!) {
  Page(page: ${'$'}page, perPage: 10) {
    pageInfo { hasNextPage currentPage }
    threads(mediaCategoryId: ${'$'}mediaCategoryId, sort: [ID_DESC]) {
      id
      title
      replyCount
      viewCount
      createdAt
      user {
        id
        name
        avatar { large }
      }
    }
  }
}
"""

private const val MediaFollowingPageQuery = """
query MediaFollowingPage(${'$'}mediaId: Int!) {
  Page(page: 1, perPage: 25) {
    mediaList(mediaId: ${'$'}mediaId, isFollowing: true) {
      id
      status
      progress
      score(format: POINT_10_DECIMAL)
      user {
        id
        name
        avatar { large }
      }
    }
  }
}
"""

private const val MediaActivitiesPageQuery = """
query MediaActivitiesPage(${'$'}page: Int!, ${'$'}mediaId: Int!, ${'$'}userId: Int, ${'$'}isFollowing: Boolean) {
  Page(page: ${'$'}page, perPage: 10) {
    pageInfo { hasNextPage currentPage }
    activities(mediaId: ${'$'}mediaId, userId: ${'$'}userId, isFollowing: ${'$'}isFollowing, type: MEDIA_LIST, sort: [ID_DESC]) {
      ... on ListActivity {
        id
        status
        progress
        createdAt
        likeCount
        user {
          id
          name
          avatar { large }
        }
      }
    }
  }
}
"""

private const val MediaRecommendationsPageQuery = """
query MediaRecommendationsPage(${'$'}mediaId: Int!) {
  Media(id: ${'$'}mediaId) {
    id
    recommendations(page: 1, perPage: 15, sort: [RATING_DESC]) {
      nodes {
        id
        rating
        mediaRecommendation {
          id
          type
          format
          seasonYear
          episodes
          chapters
          volumes
          averageScore
          favourites
          title { romaji english native userPreferred }
          coverImage { extraLarge large medium color }
          mediaListEntry { status }
        }
      }
    }
  }
}
"""

@Serializable
private data class MediaUserAvatarDto(
    val large: String? = null,
)

@Serializable
private data class MediaUserDto(
    val id: Int,
    val name: String,
    val avatar: MediaUserAvatarDto? = null,
)

@Serializable
private data class MediaReviewsPageResponseDto(
    val Media: MediaReviewsPageWrapperDto? = null,
)

@Serializable
private data class MediaReviewsPageWrapperDto(
    val id: Int,
    val reviews: MediaReviewsConnectionDto? = null,
)

@Serializable
private data class MediaReviewsConnectionDto(
    val pageInfo: MediaPageInfoDto? = null,
    val nodes: List<MediaReviewNodeDto> = emptyList(),
)

@Serializable
private data class MediaReviewNodeDto(
    val id: Int,
    val summary: String? = null,
    val rating: Int = 0,
    val ratingAmount: Int = 0,
    val body: String? = null,
    val user: MediaUserDto? = null,
)

@Serializable
private data class MediaThreadsPageResponseDto(
    val Page: MediaThreadsPageWrapperDto? = null,
)

@Serializable
private data class MediaThreadsPageWrapperDto(
    val pageInfo: MediaPageInfoDto? = null,
    val threads: List<MediaThreadNodeDto> = emptyList(),
)

@Serializable
private data class MediaThreadNodeDto(
    val id: Int,
    val title: String? = null,
    val replyCount: Int = 0,
    val viewCount: Int = 0,
    val createdAt: Long = 0L,
    val user: MediaUserDto? = null,
)

@Serializable
private data class MediaFollowingPageResponseDto(
    val Page: MediaFollowingPageWrapperDto? = null,
)

@Serializable
private data class MediaFollowingPageWrapperDto(
    val mediaList: List<MediaFollowingNodeDto> = emptyList(),
)

@Serializable
private data class MediaFollowingNodeDto(
    val id: Int,
    val status: String? = null,
    val progress: Int = 0,
    val score: Double = 0.0,
    val user: MediaUserDto? = null,
)

@Serializable
private data class MediaActivitiesPageResponseDto(
    val Page: MediaActivitiesPageWrapperDto? = null,
)

@Serializable
private data class MediaActivitiesPageWrapperDto(
    val pageInfo: MediaPageInfoDto? = null,
    val activities: List<MediaActivityNodeDto> = emptyList(),
)

@Serializable
private data class MediaActivityNodeDto(
    val id: Int,
    val status: String? = null,
    val progress: Int = 0,
    val createdAt: Long = 0L,
    val likeCount: Int = 0,
    val user: MediaUserDto? = null,
)

@Serializable
private data class MediaRecommendationsPageResponseDto(
    val Media: MediaRecommendationsPageWrapperDto? = null,
)

@Serializable
private data class MediaRecommendationsPageWrapperDto(
    val id: Int,
    val recommendations: MediaRecommendationsConnectionDto? = null,
)

@Serializable
private data class MediaRecommendationsConnectionDto(
    val nodes: List<MediaRecommendationNodeDto> = emptyList(),
)

@Serializable
private data class MediaRecommendationNodeDto(
    val id: Int,
    val rating: Int = 0,
    val mediaRecommendation: MediaRecommendationMediaDto? = null,
)

@Serializable
private data class MediaRecommendationListEntryDto(
    val status: String? = null,
)

@Serializable
private data class MediaRecommendationMediaDto(
    val id: Int,
    val title: com.luum.michi.app.core.anilist.dto.MediaTitleDto? = null,
    val coverImage: com.luum.michi.app.core.anilist.dto.MediaCoverImageDto? = null,
    val format: String? = null,
    val seasonYear: Int? = null,
    val episodes: Int? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val averageScore: Int? = null,
    val favourites: Int? = null,
    val mediaListEntry: MediaRecommendationListEntryDto? = null,
)


@Serializable
private data class MediaCharactersPageResponseDto(
    val Media: MediaCharactersPageWrapperDto? = null,
)

@Serializable
private data class MediaCharactersPageWrapperDto(
    val id: Int,
    val characters: MediaCharacterConnectionDto? = null,
)

@Serializable
private data class MediaStaffPageResponseDto(
    val Media: MediaStaffPageWrapperDto? = null,
)

@Serializable
private data class MediaStaffPageWrapperDto(
    val id: Int,
    val staff: MediaStaffConnectionDto? = null,
)

internal class MediaDetailRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : MediaDetailRepository {

    override suspend fun loadDetail(
        mediaId: Int,
        voiceLanguage: String,
    ): NetworkResult<MediaDetail> {
        val request = AniListGraphQLRequest(
            query = MediaDetailQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(mediaId),
                    "voiceLanguage" to JsonPrimitive(voiceLanguage),
                ),
            ),
            operationName = "MediaDetail",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaDetailResponseDto.serializer(), dataJson)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val media = result.value.media
                if (media == null) {
                    NetworkResult.Failure(
                        NetworkError.GraphQL(listOf("Media not found for id=$mediaId")),
                    )
                } else {
                    NetworkResult.Success(media.toDomain())
                }
            }
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun loadCharactersPage(
        mediaId: Int,
        page: Int,
        voiceLanguage: String,
    ): NetworkResult<MediaCharactersPage> {
        val request = AniListGraphQLRequest(
            query = MediaCharactersPageQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(mediaId),
                    "page" to JsonPrimitive(page),
                    "voiceLanguage" to JsonPrimitive(voiceLanguage),
                ),
            ),
            operationName = "MediaCharactersPage",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaCharactersPageResponseDto.serializer(), dataJson)
        }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(
                result.value.Media?.characters.toCharactersPage(page),
            )
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun loadStaffPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaStaffPage> {
        val request = AniListGraphQLRequest(
            query = MediaStaffPageQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(mediaId),
                    "page" to JsonPrimitive(page),
                ),
            ),
            operationName = "MediaStaffPage",
        )

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaStaffPageResponseDto.serializer(), dataJson)
        }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(
                result.value.Media?.staff.toStaffPage(page),
            )
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun loadReviewsPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaReviewsPage> {
        val request = AniListGraphQLRequest(
            query = MediaReviewsPageQuery,
            variables = JsonObject(
                mapOf(
                    "mediaId" to JsonPrimitive(mediaId),
                    "page" to JsonPrimitive(page),
                ),
            ),
            operationName = "MediaReviewsPage",
        )
        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaReviewsPageResponseDto.serializer(), dataJson)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val reviewsConnection = result.value.Media?.reviews
                val items = reviewsConnection?.nodes?.map { node ->
                    MediaReviewEntry(
                        id = node.id,
                        summary = node.summary.orEmpty(),
                        rating = "${node.rating}/${node.ratingAmount}",
                        reviewerName = node.user?.name.orEmpty(),
                        reviewerImageUrl = node.user?.avatar?.large,
                        text = node.body.orEmpty(),
                    )
                }.orEmpty()
                NetworkResult.Success(
                    MediaReviewsPage(
                        items = items,
                        hasNextPage = reviewsConnection?.pageInfo?.hasNextPage ?: false,
                        currentPage = reviewsConnection?.pageInfo?.currentPage ?: page,
                    )
                )
            }
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun loadThreadsPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaThreadsPage> {
        val request = AniListGraphQLRequest(
            query = MediaThreadsPageQuery,
            variables = JsonObject(
                mapOf(
                    "mediaCategoryId" to JsonPrimitive(mediaId),
                    "page" to JsonPrimitive(page),
                ),
            ),
            operationName = "MediaThreadsPage",
        )
        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaThreadsPageResponseDto.serializer(), dataJson)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val pageData = result.value.Page
                val items = pageData?.threads?.map { node ->
                    MediaThreadEntry(
                        id = node.id,
                        title = node.title.orEmpty(),
                        authorName = node.user?.name.orEmpty(),
                        authorImageUrl = node.user?.avatar?.large,
                        replyCount = node.replyCount,
                        viewCount = node.viewCount,
                        createdAt = node.createdAt,
                    )
                }.orEmpty()
                NetworkResult.Success(
                    MediaThreadsPage(
                        items = items,
                        hasNextPage = pageData?.pageInfo?.hasNextPage ?: false,
                        currentPage = pageData?.pageInfo?.currentPage ?: page,
                    )
                )
            }
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun loadFollowingEntries(
        mediaId: Int,
    ): NetworkResult<List<MediaFollowingEntry>> {
        val request = AniListGraphQLRequest(
            query = MediaFollowingPageQuery,
            variables = JsonObject(
                mapOf(
                    "mediaId" to JsonPrimitive(mediaId),
                ),
            ),
            operationName = "MediaFollowingPage",
        )
        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaFollowingPageResponseDto.serializer(), dataJson)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val items = result.value.Page?.mediaList?.map { node ->
                    val statusLabel = when (node.status?.uppercase()) {
                        "CURRENT" -> "Viendo"
                        "COMPLETED" -> "Completado"
                        "PLANNING" -> "Pendiente"
                        "PAUSED" -> "Pausado"
                        "DROPPED" -> "Dropeado"
                        "REPEATING" -> "Repitiendo"
                        else -> node.status.orEmpty()
                    }
                    val progressLabel = "$statusLabel: ${node.progress}"
                    val scoreLabel = if (node.score > 0.0) "Score: ${node.score}" else "Score: -"
                    MediaFollowingEntry(
                        id = node.id,
                        userName = node.user?.name.orEmpty(),
                        userImageUrl = node.user?.avatar?.large,
                        progressLabel = progressLabel,
                        scoreLabel = scoreLabel,
                    )
                }.orEmpty()
                NetworkResult.Success(items)
            }
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun loadActivitiesPage(
        mediaId: Int,
        page: Int,
        userId: Int?,
        isFollowing: Boolean?,
    ): NetworkResult<MediaActivitiesPage> {
        val variables = buildMap<String, JsonElement> {
            put("mediaId", JsonPrimitive(mediaId))
            put("page", JsonPrimitive(page))
            if (userId != null) put("userId", JsonPrimitive(userId))
            if (isFollowing != null) put("isFollowing", JsonPrimitive(isFollowing))
        }
        val request = AniListGraphQLRequest(
            query = MediaActivitiesPageQuery,
            variables = JsonObject(variables),
            operationName = "MediaActivitiesPage",
        )
        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaActivitiesPageResponseDto.serializer(), dataJson)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val pageData = result.value.Page
                val items = pageData?.activities?.map { node ->
                    val statusText = when (node.status?.uppercase()) {
                        "CURRENT" -> "watched episode"
                        "COMPLETED" -> "completed"
                        "PLANNING" -> "planning"
                        "PAUSED" -> "paused"
                        "DROPPED" -> "dropped"
                        "REPEATING" -> "repeating"
                        else -> node.status.orEmpty()
                    }
                    val progressText = if (node.progress > 0) " ${node.progress} of" else ""
                    val actionText = "$statusText$progressText"
                    
                    val formattedTime = if (node.createdAt > 0L) {
                        try {
                            val parts = millisToCalendarParts(node.createdAt * 1000L)
                            "${parts.year}-${parts.month.toString().padStart(2, '0')}-${parts.day.toString().padStart(2, '0')}"
                        } catch (e: Exception) {
                            "recently"
                        }
                    } else {
                        "recently"
                    }

                    MediaActivityEntry(
                        id = node.id,
                        userName = node.user?.name.orEmpty(),
                        userImageUrl = node.user?.avatar?.large,
                        actionText = actionText,
                        timeLabel = formattedTime,
                        likesCount = node.likeCount,
                    )
                }.orEmpty()
                NetworkResult.Success(
                    MediaActivitiesPage(
                        items = items,
                        hasNextPage = pageData?.pageInfo?.hasNextPage ?: false,
                        currentPage = pageData?.pageInfo?.currentPage ?: page,
                    )
                )
            }
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun loadRecommendations(
        mediaId: Int,
    ): NetworkResult<List<MediaRecommendationEntry>> {
        val request = AniListGraphQLRequest(
            query = MediaRecommendationsPageQuery,
            variables = JsonObject(
                mapOf(
                    "mediaId" to JsonPrimitive(mediaId),
                ),
            ),
            operationName = "MediaRecommendationsPage",
        )
        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaRecommendationsPageResponseDto.serializer(), dataJson)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val items = result.value.Media?.recommendations?.nodes?.map { node ->
                    val recMedia = node.mediaRecommendation
                    MediaRecommendationEntry(
                        id = recMedia?.id ?: 0,
                        title = recMedia?.title?.userPreferred ?: recMedia?.title?.english ?: recMedia?.title?.romaji.orEmpty(),
                        coverUrl = recMedia?.coverImage?.bestUrl,
                        format = recMedia?.format?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: recMedia?.format,
                        year = recMedia?.seasonYear,
                        episodesCount = recMedia?.episodes,
                        chaptersCount = recMedia?.chapters,
                        volumesCount = recMedia?.volumes,
                        averageScore = recMedia?.averageScore,
                        favouritesCount = recMedia?.favourites,
                        likesCount = node.rating,
                        viewerStatus = recMedia?.mediaListEntry?.status,
                    )
                }.orEmpty()
                NetworkResult.Success(items)
            }
            is NetworkResult.Failure -> result
        }
    }
}
