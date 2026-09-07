package com.luum.michi.app.mediaList.repository.manga

import com.luum.michi.app.core.repository.anilist.dto.MediaListCollectionResponseDto
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.core.domain.network.AniListGraphQLClient
import com.luum.michi.app.core.domain.network.AniListGraphQLRequest
import com.luum.michi.app.core.repository.network.AniListJson
import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.core.domain.network.map
import com.luum.michi.app.mediaList.domain.manga.model.MangaListEntry
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
        priority
        startedAt { year month day }
        completedAt { year month day }
        private
        hiddenFromStatusLists
        media {
          id
          format
          status
          chapters
          volumes
          averageScore
          popularity
          favourites
          trending
          genres
          season
          seasonYear
          startDate { year month day }
          title { romaji english native userPreferred }
          coverImage { large medium color }
        }
      }
    }
  }
}
"""

internal class MangaListRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : MangaListRepository {

    override suspend fun loadList(userId: Int): NetworkResult<List<MangaListEntry>> {
        val request = AniListGraphQLRequest(
            query = MangaListQuery,
            variables = JsonObject(mapOf("userId" to JsonPrimitive(userId))),
            operationName = "MediaListCollection",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaListCollectionResponseDto.serializer(), dataJson)
        }.map { response ->
            response.collection.lists
                .filter { !it.isCustomList }
                .flatMap { it.entries }
                .mapIndexed { index, entry -> entry.toMangaListEntry(index) }
        }
    }
}
