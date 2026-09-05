package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.anilist.dto.MediaCharacterConnectionDto
import com.luum.michi.app.mediaDetail.domain.MediaDetailRepository
import com.luum.michi.app.core.anilist.dto.MediaDetailResponseDto
import com.luum.michi.app.core.anilist.dto.MediaStaffConnectionDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.presentation.model.MediaCharactersPage
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import com.luum.michi.app.mediaDetail.presentation.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.presentation.model.MediaStaffPage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
                        coverUrl = recMedia?.coverImage?.thumbnailUrl,
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
