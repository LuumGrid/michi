package com.luum.michi.app.discovery.data

import com.luum.michi.app.core.anilist.dto.DiscoveryResponseDto
import com.luum.michi.app.core.auth.currentEpochSeconds
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val DiscoveryQuery = """
query Discovery(${'$'}from: Int!, ${'$'}to: Int!) {
  trendingAnime: Page(perPage: 20) {
    media(sort: TRENDING_DESC, type: ANIME) {
      id
      format
      episodes
      averageScore
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  trendingManga: Page(perPage: 20) {
    media(sort: TRENDING_DESC, type: MANGA) {
      id
      format
      chapters
      averageScore
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  releasingToday: Page(perPage: 25) {
    airingSchedules(airingAt_greater: ${'$'}from, airingAt_lesser: ${'$'}to, sort: TIME) {
      id
      airingAt
      episode
      media {
        id
        format
        title { romaji english native userPreferred }
        coverImage { extraLarge large medium color }
      }
    }
  }
}
"""

internal class DiscoveryRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val nowProvider: () -> Long = { currentEpochSeconds() },
) : DiscoveryRepository {

    override suspend fun loadFeed(): NetworkResult<DiscoveryFeed> {
        val from = nowProvider()
        val to = from + SecondsPerDay

        val request = AniListGraphQLRequest(
            query = DiscoveryQuery,
            variables = JsonObject(
                mapOf(
                    "from" to JsonPrimitive(from),
                    "to" to JsonPrimitive(to),
                ),
            ),
            operationName = "Discovery",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromString(DiscoveryResponseDto.serializer(), dataJson)
        }.map { it.toDiscoveryFeed() }
    }
}

private const val SecondsPerDay: Long = 86_400L
