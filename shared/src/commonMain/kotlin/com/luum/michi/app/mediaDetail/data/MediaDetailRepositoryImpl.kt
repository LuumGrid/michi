package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.anilist.dto.MediaCharacterConnectionDto
import com.luum.michi.app.core.anilist.dto.MediaDetailResponseDto
import com.luum.michi.app.core.anilist.dto.MediaStaffConnectionDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.presentation.model.MediaCharactersPage
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
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
            AniListJson.decodeFromString(MediaDetailResponseDto.serializer(), dataJson)
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
            AniListJson.decodeFromString(MediaCharactersPageResponseDto.serializer(), dataJson)
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
            AniListJson.decodeFromString(MediaStaffPageResponseDto.serializer(), dataJson)
        }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(
                result.value.Media?.staff.toStaffPage(page),
            )
            is NetworkResult.Failure -> result
        }
    }
}
