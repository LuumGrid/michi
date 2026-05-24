package com.luum.michi.app.mediaDetail.data

import com.luum.michi.app.core.anilist.dto.MediaDetailResponseDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.mediaDetail.presentation.model.MediaDetail
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
  }
}
"""

internal class MediaDetailRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : MediaDetailRepository {

    override suspend fun loadDetail(mediaId: Int): NetworkResult<MediaDetail> {
        val request = AniListGraphQLRequest(
            query = MediaDetailQuery,
            variables = JsonObject(mapOf("id" to JsonPrimitive(mediaId))),
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
}
