package com.luum.michi.app.mediaDetail.repository.media

import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.core.network.domain.requireNode
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharacterEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaEntryEditorData
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.luum.michi.app.core.model.RailPageSize


private const val MediaDetailQuery = """
query MediaDetail(${'$'}id: Int!) {
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
    popularity
    favourites
    coverImage { extraLarge large medium color }
    bannerImage
    source
    season
    seasonYear
    startDate { year month day }
    endDate { year month day }
    studios(sort: [NAME]) { edges { isMain node { id name isAnimationStudio } } }
    nextAiringEpisode { episode airingAt timeUntilAiring }
    isAdult
    isFavourite
    tags { name rank isMediaSpoiler }
    externalLinks { id site url type icon color language }
    trailer { id site thumbnail }
    synonyms
    mediaListEntry { id status progress progressVolumes score advancedScores notes repeat priority private hiddenFromStatusLists startedAt { year month day } completedAt { year month day } }
    relations {
      edges {
        relationType(version: 2)
        node {
          id
          format
          status
          title { romaji english native userPreferred }
          coverImage { extraLarge large medium }
          season
          seasonYear
          startDate { year }
        }
      }
    }
    characters(page: 1, perPage: $RailPageSize, sort: [ROLE, RELEVANCE]) {
      pageInfo { hasNextPage currentPage }
      edges {
        id
        role
        node { id name { full native userPreferred } image { large medium } }
      }
    }
    staff(page: 1, perPage: $RailPageSize, sort: [RELEVANCE]) {
      pageInfo { hasNextPage currentPage }
      edges {
        id
        role
        node { id name { full native userPreferred } image { large medium } }
      }
    }
    recommendations(page: 1, perPage: $RailPageSize, sort: [RATING_DESC]) {
      pageInfo { hasNextPage currentPage }
      nodes {
        mediaRecommendation {
          id
          type
          format
          status
          season
          seasonYear
          episodes
          chapters
          volumes
          averageScore
          favourites
          title { romaji english native userPreferred }
          coverImage { extraLarge large medium color }
        }
      }
    }
  }
}
"""

private const val MediaEntryEditorQuery = """
query MediaEntryEditor(${'$'}id: Int!) {
  Media(id: ${'$'}id) {
    id
    type
    title { romaji english native userPreferred }
    episodes
    chapters
    volumes
    isFavourite
    mediaListEntry { id status progress progressVolumes score advancedScores notes repeat priority private hiddenFromStatusLists startedAt { year month day } completedAt { year month day } }
  }
}
"""

private const val MediaCharactersPageQuery = """
query MediaCharactersPage(${'$'}id: Int!, ${'$'}page: Int!) {
  Media(id: ${'$'}id) {
    id
    characters(page: ${'$'}page, perPage: $RailPageSize, sort: [ROLE, RELEVANCE]) {
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

private const val MediaStaffPageQuery = """
query MediaStaffPage(${'$'}id: Int!, ${'$'}page: Int!) {
  Media(id: ${'$'}id) {
    id
    staff(page: ${'$'}page, perPage: $RailPageSize, sort: [RELEVANCE]) {
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
query MediaRecommendationsPage(${'$'}mediaId: Int!, ${'$'}page: Int!) {
  Media(id: ${'$'}mediaId) {
    id
    recommendations(page: ${'$'}page, perPage: $RailPageSize, sort: [RATING_DESC]) {
      pageInfo { hasNextPage currentPage }
      nodes {
        mediaRecommendation {
          id
          type
          format
          status
          season
          seasonYear
          episodes
          chapters
          volumes
          averageScore
          favourites
          title { romaji english native userPreferred }
          coverImage { extraLarge large medium color }
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
        strings: LanguageStrings,
    ): NetworkResult<MediaDetail> {
        val request = AniListGraphQLRequest(
            query = MediaDetailQuery,
            variables = JsonObject(
                mapOf("id" to JsonPrimitive(mediaId)),
            ),
            operationName = "MediaDetail",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaDetailResponseDto.serializer(), dataJson)
        }
            .map { it.media }
            .requireNode("Media", mediaId)
            .map { it.toDomain(strings) }
    }

    override suspend fun loadEditorData(mediaId: Int): NetworkResult<MediaEntryEditorData> {
        val request = AniListGraphQLRequest(
            query = MediaEntryEditorQuery,
            variables = JsonObject(
                mapOf("id" to JsonPrimitive(mediaId)),
            ),
            operationName = "MediaEntryEditor",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaDetailResponseDto.serializer(), dataJson)
        }
            .map { it.media }
            .requireNode("Media", mediaId)
            .map { it.toEditorData() }
    }

    override suspend fun loadCharactersPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<Page<MediaCharacterEntry>> {
        val request = AniListGraphQLRequest(
            query = MediaCharactersPageQuery,
            variables = JsonObject(
                mapOf(
                    "id" to JsonPrimitive(mediaId),
                    "page" to JsonPrimitive(page),
                ),
            ),
            operationName = "MediaCharactersPage",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaCharactersPageResponseDto.serializer(), dataJson)
        }
            .map { it.Media }
            .requireNode("Media", mediaId)
            .map { it.characters.toCharactersPage(page) }
    }

    override suspend fun loadStaffPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<Page<MediaStaffEntry>> {
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

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaStaffPageResponseDto.serializer(), dataJson)
        }
            .map { it.Media }
            .requireNode("Media", mediaId)
            .map { it.staff.toStaffPage(page) }
    }

    override suspend fun loadRecommendations(
        mediaId: Int,
        page: Int,
    ): NetworkResult<Page<MediaRecommendationEntry>> {
        val request = AniListGraphQLRequest(
            query = MediaRecommendationsPageQuery,
            variables = JsonObject(
                mapOf(
                    "mediaId" to JsonPrimitive(mediaId),
                    "page" to JsonPrimitive(page),
                ),
            ),
            operationName = "MediaRecommendationsPage",
        )
        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaRecommendationsPageResponseDto.serializer(), dataJson)
        }
            .map { it.Media?.recommendations.toRecommendationsPage(fallbackPage = page) }
    }
}
