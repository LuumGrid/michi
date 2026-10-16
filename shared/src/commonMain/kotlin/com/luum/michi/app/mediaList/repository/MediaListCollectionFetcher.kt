package com.luum.michi.app.mediaList.repository

import com.luum.michi.app.core.model.MediaType
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.core.network.repository.AniListJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * One query for both user lists: `$type` decides anime vs manga, and the node
 * requests the union of the fields the two mappers read (episodes + next airing
 * for anime, chapters/volumes/progressVolumes for manga). Fields no mapper or
 * model reads (group name/status, entry notes/private/hiddenFromStatusLists,
 * timeUntilAiring) are left out.
 */
private const val MediaListCollectionQuery = """
query MediaListCollection(${'$'}userId: Int!, ${'$'}type: MediaType!) {
  MediaListCollection(userId: ${'$'}userId, type: ${'$'}type) {
    lists {
      isCustomList
      entries {
        id
        status
        score
        progress
        progressVolumes
        updatedAt
        priority
        startedAt { year month day }
        completedAt { year month day }
        media {
          id
          format
          status
          episodes
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
          isAdult
          coverImage { extraLarge large medium color }
          nextAiringEpisode { episode airingAt }
        }
      }
    }
  }
}
"""

/**
 * The single fetch both list repositories share: the user's full collection for
 * [type], flattened out of its custom lists. The caller maps the entries.
 */
internal suspend fun AniListGraphQLClient.fetchMediaListCollection(
    userId: Int,
    type: MediaType,
): NetworkResult<List<MediaListEntryDto>> {
    val request = AniListGraphQLRequest(
        query = MediaListCollectionQuery,
        variables = JsonObject(
            mapOf(
                "userId" to JsonPrimitive(userId),
                "type" to JsonPrimitive(type.name),
            ),
        ),
        operationName = "MediaListCollection",
    )
    return execute(request) { dataJson ->
        AniListJson.decodeFromJsonElement(MediaListCollectionResponseDto.serializer(), dataJson)
    }.map { response ->
        response.collection.lists
            .filter { !it.isCustomList }
            .flatMap { it.entries }
    }
}
