package com.luum.michi.app.browse.data

import com.luum.michi.app.core.anilist.dto.BrowseResponseDto
import com.luum.michi.app.core.media.MediaSeasonYear
import com.luum.michi.app.core.media.currentSeasonAndYear
import com.luum.michi.app.core.media.next
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val BrowseQuery = """
query Browse(
  ${'$'}currentSeason: MediaSeason!,
  ${'$'}currentYear: Int!,
  ${'$'}nextSeason: MediaSeason!,
  ${'$'}nextYear: Int!
) {
  popularThisSeason: Page(perPage: 20) {
    media(type: ANIME, season: ${'$'}currentSeason, seasonYear: ${'$'}currentYear, sort: POPULARITY_DESC) {
      id format episodes averageScore
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  upcomingNextSeason: Page(perPage: 20) {
    media(type: ANIME, season: ${'$'}nextSeason, seasonYear: ${'$'}nextYear, sort: POPULARITY_DESC) {
      id format episodes averageScore
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  allTimePopularAnime: Page(perPage: 20) {
    media(type: ANIME, sort: POPULARITY_DESC) {
      id format episodes averageScore
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  allTimePopularManga: Page(perPage: 20) {
    media(type: MANGA, sort: POPULARITY_DESC) {
      id format chapters averageScore
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  topAnime: Page(perPage: 20) {
    media(type: ANIME, sort: SCORE_DESC) {
      id format episodes averageScore
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  topManga: Page(perPage: 20) {
    media(type: MANGA, sort: SCORE_DESC) {
      id format chapters averageScore
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
}
"""

internal class BrowseRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val seasonProvider: () -> MediaSeasonYear = { currentSeasonAndYear() },
) : BrowseRepository {

    override suspend fun loadFeed(): NetworkResult<BrowseFeed> {
        val current = seasonProvider()
        val next = current.next()

        val request = AniListGraphQLRequest(
            query = BrowseQuery,
            variables = JsonObject(
                mapOf(
                    "currentSeason" to JsonPrimitive(current.season.name),
                    "currentYear" to JsonPrimitive(current.year),
                    "nextSeason" to JsonPrimitive(next.season.name),
                    "nextYear" to JsonPrimitive(next.year),
                ),
            ),
            operationName = "Browse",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromString(BrowseResponseDto.serializer(), dataJson)
        }.map { it.toBrowseFeed() }
    }
}
