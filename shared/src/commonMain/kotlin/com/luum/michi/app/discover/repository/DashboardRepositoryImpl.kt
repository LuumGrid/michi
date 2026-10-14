package com.luum.michi.app.discover.repository

import com.luum.michi.app.core.network.repository.dto.DashboardResponseDto
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.discover.domain.DashboardFeed
import com.luum.michi.app.discover.domain.DashboardRepository
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.core.model.MediaSeasonYear
import com.luum.michi.app.core.model.currentSeasonAndYear
import com.luum.michi.app.core.model.next
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.RailPageSize
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/**
 * Popularity floor for the "best rated" shelves: pure SCORE_DESC surfaces
 * obscure 90-score titles with a handful of votes, so a floor keeps them
 * recognizable (same idea as the New releases shelf).
 */
private const val BestRatedMinPopularity = 5000

/** Popularity floor for the (narrower, 12-month) New releases shelf. */
private const val NewReleasesMinPopularity = 1000

private const val MediaFields = """
      id format episodes chapters
      title { romaji english native userPreferred }
      coverImage { extraLarge large medium color }
"""

private val AnimeDashboardQuery = """
query AnimeDashboard(
  ${'$'}currentSeason: MediaSeason!,
  ${'$'}currentYear: Int!,
  ${'$'}nextSeason: MediaSeason!,
  ${'$'}nextYear: Int!,
  ${'$'}recentStart: FuzzyDateInt,
  ${'$'}isAdult: Boolean
) {
  seasonal: Page(perPage: $RailPageSize) {
    media(type: ANIME, season: ${'$'}currentSeason, seasonYear: ${'$'}currentYear, sort: POPULARITY_DESC, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  upcomingSeason: Page(perPage: $RailPageSize) {
    media(type: ANIME, season: ${'$'}nextSeason, seasonYear: ${'$'}nextYear, sort: POPULARITY_DESC, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  trendingAnime: Page(perPage: $RailPageSize) {
    media(type: ANIME, sort: TRENDING_DESC, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  popularAnime: Page(perPage: $RailPageSize) {
    media(type: ANIME, format_in: [TV, TV_SHORT, OVA, ONA, SPECIAL], sort: POPULARITY_DESC, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  popularMovie: Page(perPage: $RailPageSize) {
    media(type: ANIME, format: MOVIE, sort: POPULARITY_DESC, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  bestAnime: Page(perPage: $RailPageSize) {
    media(type: ANIME, format_in: [TV, TV_SHORT, OVA, ONA, SPECIAL], sort: SCORE_DESC, popularity_greater: $BestRatedMinPopularity, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  bestMovie: Page(perPage: $RailPageSize) {
    media(type: ANIME, format: MOVIE, sort: SCORE_DESC, popularity_greater: $BestRatedMinPopularity, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  bestAnimeNow: Page(perPage: $RailPageSize) {
    media(type: ANIME, format_in: [TV, TV_SHORT, OVA, ONA, SPECIAL], status: RELEASING, sort: SCORE_DESC, popularity_greater: $BestRatedMinPopularity, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  bestMovieNow: Page(perPage: $RailPageSize) {
    media(type: ANIME, format: MOVIE, startDate_greater: ${'$'}recentStart, sort: SCORE_DESC, popularity_greater: $BestRatedMinPopularity, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
}
"""

private val MangaDashboardQuery = """
query MangaDashboard(
  ${'$'}recentMangaStart: FuzzyDateInt,
  ${'$'}isAdult: Boolean
) {
  newReleasesManga: Page(perPage: $RailPageSize) {
    media(type: MANGA, startDate_greater: ${'$'}recentMangaStart, popularity_greater: $NewReleasesMinPopularity, sort: SCORE_DESC, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  trendingManga: Page(perPage: $RailPageSize) {
    media(type: MANGA, sort: TRENDING_DESC, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  popularManga: Page(perPage: $RailPageSize) {
    media(type: MANGA, countryOfOrigin: JP, format_in: [MANGA, ONE_SHOT], sort: POPULARITY_DESC, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  popularLightNovel: Page(perPage: $RailPageSize) {
    media(type: MANGA, format: NOVEL, sort: POPULARITY_DESC, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  popularManhwa: Page(perPage: $RailPageSize) {
    media(type: MANGA, countryOfOrigin: KR, format_in: [MANGA, ONE_SHOT], sort: POPULARITY_DESC, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  popularManhua: Page(perPage: $RailPageSize) {
    media(type: MANGA, countryOfOrigin_in: [CN, TW], format_in: [MANGA, ONE_SHOT], sort: POPULARITY_DESC, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  bestManga: Page(perPage: $RailPageSize) {
    media(type: MANGA, countryOfOrigin: JP, format_in: [MANGA, ONE_SHOT], sort: SCORE_DESC, popularity_greater: $BestRatedMinPopularity, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  bestLightNovel: Page(perPage: $RailPageSize) {
    media(type: MANGA, format: NOVEL, sort: SCORE_DESC, popularity_greater: $BestRatedMinPopularity, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  bestManhwa: Page(perPage: $RailPageSize) {
    media(type: MANGA, countryOfOrigin: KR, format_in: [MANGA, ONE_SHOT], sort: SCORE_DESC, popularity_greater: $BestRatedMinPopularity, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
  bestManhua: Page(perPage: $RailPageSize) {
    media(type: MANGA, countryOfOrigin_in: [CN, TW], format_in: [MANGA, ONE_SHOT], sort: SCORE_DESC, popularity_greater: $BestRatedMinPopularity, isAdult: ${'$'}isAdult) {
$MediaFields
    }
  }
}
"""

internal class DashboardRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val seasonProvider: () -> MediaSeasonYear = { currentSeasonAndYear() },
) : DashboardRepository {

    override suspend fun loadFeed(
        mediaType: DashboardMediaType,
        hideAdult: Boolean,
        strings: LanguageStrings,
    ): NetworkResult<DashboardFeed> {
        val variables = mutableMapOf<String, JsonElement>()
        val query: String
        val operation: String
        when (mediaType) {
            DashboardMediaType.ANIME -> {
                val current = seasonProvider()
                val next = current.next()
                query = AnimeDashboardQuery
                operation = "AnimeDashboard"
                variables["currentSeason"] = JsonPrimitive(current.season.name)
                variables["currentYear"] = JsonPrimitive(current.year)
                variables["nextSeason"] = JsonPrimitive(next.season.name)
                variables["nextYear"] = JsonPrimitive(next.year)
                variables["recentStart"] = JsonPrimitive(recentStartFuzzy())
            }
            DashboardMediaType.MANGA -> {
                query = MangaDashboardQuery
                operation = "MangaDashboard"
                variables["recentMangaStart"] = JsonPrimitive(recentStartFuzzy())
            }
        }
        // Omit (null) when adult is allowed: the server then returns both.
        // Never send true — that would filter to adult ONLY.
        if (hideAdult) {
            variables["isAdult"] = JsonPrimitive(false)
        }

        val request = AniListGraphQLRequest(
            query = query,
            variables = JsonObject(variables),
            operationName = operation,
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(DashboardResponseDto.serializer(), dataJson)
        }.map { it.toDashboardFeed(strings) }
    }
}

/** FuzzyDateInt (YYYYMMDD) one year back: the "recent" shelf window. */
private fun recentStartFuzzy(now: Instant = Clock.System.now()): Int {
    val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val year = date.year - 1
    return year * 10000 + date.month.number * 100 + date.day
}
