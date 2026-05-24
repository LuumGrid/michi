package com.luum.michi.app.dashboard.data

import com.luum.michi.app.core.anilist.dto.DashboardResponseDto
import com.luum.michi.app.core.auth.currentEpochSeconds
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

private const val DashboardQuery = """
query Dashboard(
  ${'$'}from: Int!,
  ${'$'}to: Int!,
  ${'$'}currentSeason: MediaSeason!,
  ${'$'}currentYear: Int!,
  ${'$'}nextSeason: MediaSeason!,
  ${'$'}nextYear: Int!
) {
  trendingAnime: Page(perPage: 20) {
    media(sort: TRENDING_DESC, type: ANIME) {
      id format episodes averageScore favourites isFavourite mediaListEntry { id score }
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  trendingManga: Page(perPage: 20) {
    media(sort: TRENDING_DESC, type: MANGA) {
      id format chapters averageScore favourites isFavourite mediaListEntry { id score }
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  releasingToday: Page(perPage: 25) {
    airingSchedules(airingAt_greater: ${'$'}from, airingAt_lesser: ${'$'}to, sort: TIME) {
      id airingAt episode
      media {
        id format averageScore favourites isFavourite mediaListEntry { id score }
        title { romaji english native userPreferred }
        coverImage { extraLarge large medium color }
      }
    }
  }
  popularThisSeason: Page(perPage: 20) {
    media(type: ANIME, season: ${'$'}currentSeason, seasonYear: ${'$'}currentYear, sort: POPULARITY_DESC) {
      id format episodes averageScore favourites isFavourite mediaListEntry { id score }
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  upcomingNextSeason: Page(perPage: 20) {
    media(type: ANIME, season: ${'$'}nextSeason, seasonYear: ${'$'}nextYear, sort: POPULARITY_DESC) {
      id format episodes averageScore favourites isFavourite mediaListEntry { id score }
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  allTimePopularAnime: Page(perPage: 20) {
    media(type: ANIME, sort: POPULARITY_DESC) {
      id format episodes averageScore favourites isFavourite mediaListEntry { id score }
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  allTimePopularManga: Page(perPage: 20) {
    media(type: MANGA, sort: POPULARITY_DESC) {
      id format chapters averageScore favourites isFavourite mediaListEntry { id score }
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  topAnime: Page(perPage: 20) {
    media(type: ANIME, sort: SCORE_DESC) {
      id format episodes averageScore favourites isFavourite mediaListEntry { id score }
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
  topManga: Page(perPage: 20) {
    media(type: MANGA, sort: SCORE_DESC) {
      id format chapters averageScore favourites isFavourite mediaListEntry { id score }
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
    }
  }
}
"""

internal class DashboardRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val nowProvider: () -> Long = { currentEpochSeconds() },
    private val seasonProvider: () -> MediaSeasonYear = { currentSeasonAndYear() },
) : DashboardRepository {

    override suspend fun loadFeed(): NetworkResult<DashboardFeed> {
        val current = seasonProvider()
        val next = current.next()
        val from = nowProvider()
        val to = from + SecondsPerDay

        val request = AniListGraphQLRequest(
            query = DashboardQuery,
            variables = JsonObject(
                mapOf(
                    "from" to JsonPrimitive(from),
                    "to" to JsonPrimitive(to),
                    "currentSeason" to JsonPrimitive(current.season.name),
                    "currentYear" to JsonPrimitive(current.year),
                    "nextSeason" to JsonPrimitive(next.season.name),
                    "nextYear" to JsonPrimitive(next.year),
                ),
            ),
            operationName = "Dashboard",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromString(DashboardResponseDto.serializer(), dataJson)
        }.map { it.toDashboardFeed() }
    }
}

private const val SecondsPerDay: Long = 86_400L
