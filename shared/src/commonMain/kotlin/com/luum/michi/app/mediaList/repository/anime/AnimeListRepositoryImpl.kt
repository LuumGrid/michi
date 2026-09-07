package com.luum.michi.app.mediaList.repository.anime

import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListEntry
import com.luum.michi.app.core.repository.anilist.dto.MediaListCollectionResponseDto
import com.luum.michi.app.core.domain.network.AniListGraphQLClient
import com.luum.michi.app.core.domain.network.AniListGraphQLRequest
import com.luum.michi.app.core.repository.network.AniListJson
import com.luum.michi.app.core.domain.network.NetworkResult
import com.luum.michi.app.core.domain.network.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val AnimeListQuery = """
query MediaListCollection(${'$'}userId: Int!) {
  MediaListCollection(userId: ${'$'}userId, type: ANIME) {
    lists {
      name
      status
      isCustomList
      entries {
        id
        status
        score
        progress
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
          episodes
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
          nextAiringEpisode { episode airingAt timeUntilAiring }
        }
      }
    }
  }
}
"""

internal class AnimeListRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : AnimeListRepository {

    override suspend fun loadList(userId: Int): NetworkResult<List<AnimeListEntry>> {
        val request = AniListGraphQLRequest(
            query = AnimeListQuery,
            variables = JsonObject(mapOf("userId" to JsonPrimitive(userId))),
            operationName = "MediaListCollection",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaListCollectionResponseDto.serializer(), dataJson)
        }.map { response ->
            response.collection.lists
                .filter { !it.isCustomList }
                .flatMap { it.entries }
                .mapIndexed { index, entry -> entry.toAnimeListEntry(index) }
        }
    }
}
