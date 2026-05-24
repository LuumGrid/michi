package com.luum.michi.app.reading.data

import com.luum.michi.app.core.anilist.dto.MediaListCollectionResponseDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import com.luum.michi.app.reading.presentation.model.ReadingListEntry
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val MangaListQuery = """
query MediaListCollection(${'$'}userId: Int!) {
  MediaListCollection(userId: ${'$'}userId, type: MANGA) {
    lists {
      name
      status
      isCustomList
      entries {
        id
        status
        score
        progress
        progressVolumes
        notes
        updatedAt
        private
        hiddenFromStatusLists
        media {
          id
          format
          status
          chapters
          volumes
          averageScore
          title { romaji english native userPreferred }
          coverImage { extraLarge large medium color }
          bannerImage
          nextAiringEpisode { episode airingAt timeUntilAiring }
        }
      }
    }
  }
}
"""

internal class ReadingListRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : ReadingListRepository {

    override suspend fun loadList(userId: Int): NetworkResult<List<ReadingListEntry>> {
        val request = AniListGraphQLRequest(
            query = MangaListQuery,
            variables = JsonObject(mapOf("userId" to JsonPrimitive(userId))),
            operationName = "MediaListCollection",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromString(MediaListCollectionResponseDto.serializer(), dataJson)
        }.map { response ->
            response.collection.lists
                .filter { !it.isCustomList }
                .flatMap { it.entries }
                .map { it.toReadingListEntry() }
        }
    }
}
