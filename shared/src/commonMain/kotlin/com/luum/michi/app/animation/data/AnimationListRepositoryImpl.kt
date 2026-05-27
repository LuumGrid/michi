package com.luum.michi.app.animation.data

import com.luum.michi.app.animation.presentation.model.AnimationListEntry
import com.luum.michi.app.core.anilist.dto.MediaListCollectionResponseDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

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

internal class AnimationListRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : AnimationListRepository {

    override suspend fun loadList(userId: Int): NetworkResult<List<AnimationListEntry>> {
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
                .mapIndexed { index, entry -> entry.toAnimationListEntry(index) }
        }
    }
}
