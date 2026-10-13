package com.luum.michi.app.mediaDetail.repository.media

import com.luum.michi.app.core.network.repository.dto.MediaCharacterConnectionDto
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.core.network.repository.dto.MediaDetailResponseDto
import com.luum.michi.app.core.network.repository.dto.MediaPageInfoDto
import com.luum.michi.app.core.network.repository.dto.MediaStaffConnectionDto
import com.luum.michi.app.core.network.repository.dto.bestTitle
import com.luum.michi.app.core.model.parseMediaSeason
import com.luum.michi.app.core.model.parseMediaWorkStatus
import com.luum.michi.app.core.model.toTitleCase
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.media.model.MediaCharactersPage
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetail
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationEntry
import com.luum.michi.app.mediaDetail.domain.media.model.MediaRecommendationsPage
import com.luum.michi.app.mediaDetail.domain.media.model.MediaStaffPage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val CharactersPerPage = 12
private const val StaffPerPage = 12
private const val RecommendationsPerPage = 20

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
    studios { nodes { id name } }
    nextAiringEpisode { episode airingAt timeUntilAiring }
    countryOfOrigin
    isAdult
    isFavourite
    rankings { rank type context format season year allTime }
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
          type
          format
          status
          title { romaji english native userPreferred }
          coverImage { extraLarge large medium color }
          averageScore
          favourites
          season
          seasonYear
          startDate { year month day }
          mediaListEntry { status }
        }
      }
    }
    characters(page: 1, perPage: $CharactersPerPage, sort: [ROLE, RELEVANCE]) {
      pageInfo { hasNextPage currentPage }
      edges {
        id
        role
        node { id name { full native userPreferred } image { large medium } }
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
query MediaCharactersPage(${'$'}id: Int!, ${'$'}page: Int!) {
  Media(id: ${'$'}id) {
    id
    characters(page: ${'$'}page, perPage: $CharactersPerPage, sort: [ROLE, RELEVANCE]) {
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
query MediaRecommendationsPage(${'$'}mediaId: Int!, ${'$'}page: Int!) {
  Media(id: ${'$'}mediaId) {
    id
    recommendations(page: ${'$'}page, perPage: $RecommendationsPerPage, sort: [RATING_DESC]) {
      pageInfo { hasNextPage currentPage }
      nodes {
        id
        rating
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
    val pageInfo: MediaPageInfoDto? = null,
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
    val title: com.luum.michi.app.core.network.repository.dto.MediaTitleDto? = null,
    val coverImage: com.luum.michi.app.core.network.repository.dto.MediaCoverImageDto? = null,
    val format: String? = null,
    val status: String? = null,
    val season: String? = null,
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
        strings: LanguageStrings,
    ): NetworkResult<MediaDetail> {
        val request = AniListGraphQLRequest(
            query = MediaDetailQuery,
            variables = JsonObject(
                mapOf("id" to JsonPrimitive(mediaId)),
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
                    NetworkResult.Success(media.toDomain(strings))
                }
            }
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun loadCharactersPage(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaCharactersPage> {
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

        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaCharactersPageResponseDto.serializer(), dataJson)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val media = result.value.Media
                if (media == null) {
                    NetworkResult.Failure(
                        NetworkError.GraphQL(listOf("Media not found for id=$mediaId")),
                    )
                } else {
                    NetworkResult.Success(media.characters.toCharactersPage(page))
                }
            }
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
            is NetworkResult.Success -> {
                val media = result.value.Media
                if (media == null) {
                    NetworkResult.Failure(
                        NetworkError.GraphQL(listOf("Media not found for id=$mediaId")),
                    )
                } else {
                    NetworkResult.Success(media.staff.toStaffPage(page))
                }
            }
            is NetworkResult.Failure -> result
        }
    }

    override suspend fun loadRecommendations(
        mediaId: Int,
        page: Int,
    ): NetworkResult<MediaRecommendationsPage> {
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
        val result = graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaRecommendationsPageResponseDto.serializer(), dataJson)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val connection = result.value.Media?.recommendations
                // Un mismo media puede venir repetido (un nodo por recomendación
                // de usuario): se colapsa por id para no romper las keys del grid.
                val items = connection?.nodes?.mapNotNull { node ->
                    val recMedia = node.mediaRecommendation ?: return@mapNotNull null
                    MediaRecommendationEntry(
                        id = recMedia.id,
                        title = recMedia.title.bestTitle(),
                        coverUrl = recMedia.coverImage?.thumbnailUrl,
                        format = recMedia.format?.toTitleCase(),
                        season = parseMediaSeason(recMedia.season),
                        year = recMedia.seasonYear,
                        workStatus = parseMediaWorkStatus(recMedia.status),
                        episodesCount = recMedia.episodes,
                        chaptersCount = recMedia.chapters,
                        volumesCount = recMedia.volumes,
                        averageScore = recMedia.averageScore,
                        favouritesCount = recMedia.favourites,
                        likesCount = node.rating,
                        viewerStatus = recMedia.mediaListEntry?.status,
                    )
                }.orEmpty().distinctBy { it.id }
                NetworkResult.Success(
                    MediaRecommendationsPage(
                        items = items,
                        hasNextPage = connection?.pageInfo?.hasNextPage == true,
                        currentPage = connection?.pageInfo?.currentPage ?: page,
                    ),
                )
            }
            is NetworkResult.Failure -> result
        }
    }
}
