package com.luum.michi.app.calendar.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.luum.michi.app.calendar.domain.CalendarFeed
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.core.network.repository.dto.AiringScheduleDto
import com.luum.michi.app.core.network.repository.dto.AiringSchedulePageDto
import com.luum.michi.app.core.auth.domain.currentEpochSeconds
import com.luum.michi.app.core.model.currentSeasonAndYear
import com.luum.michi.app.core.model.next
import com.luum.michi.app.core.model.startEpochSeconds
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.domain.NetworkResult

private const val CalendarQuery = """
query Calendar(${'$'}from: Int!, ${'$'}to: Int!, ${'$'}page: Int!) {
  Page(page: ${'$'}page, perPage: 100) {
    pageInfo { hasNextPage }
    airingSchedules(airingAt_greater: ${'$'}from, airingAt_lesser: ${'$'}to, sort: TIME) {
      id
      airingAt
      episode
      media {
        id
        format
        season
        seasonYear
        episodes
        averageScore
        favourites
        popularity
        title { romaji english native userPreferred }
        coverImage { extraLarge large medium color }
        isFavourite
        mediaListEntry { status progress score }
        externalLinks {
          id
          site
          url
          type
          icon
          color
          language
          isDisabled
        }
      }
    }
  }
}
"""

@Serializable
private data class CalendarResponseDto(
    @SerialName("Page") val page: AiringSchedulePageDto? = null,
)

internal class CalendarRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val nowProvider: () -> Long = { currentEpochSeconds() },
) : CalendarRepository {

    override fun loadFeed(): Flow<NetworkResult<CalendarFeed>> = flow {
        val now = nowProvider()
        val nextNextSeason = currentSeasonAndYear().next().next()
        val to = nextNextSeason.startEpochSeconds() - 1

        val allSchedules = mutableListOf<AiringScheduleDto>()
        var page = 1
        var hasNextPage = true

        while (hasNextPage) {
            val request = AniListGraphQLRequest(
                query = CalendarQuery,
                variables = JsonObject(
                    mapOf(
                        "from" to JsonPrimitive(now),
                        "to" to JsonPrimitive(to),
                        "page" to JsonPrimitive(page),
                    ),
                ),
                operationName = "Calendar",
            )
            val result = graphQLClient.execute(request) { dataJson ->
                AniListJson.decodeFromJsonElement(CalendarResponseDto.serializer(), dataJson)
            }
            when (result) {
                is NetworkResult.Success -> {
                    val pageData = result.value.page
                    allSchedules.addAll(pageData?.airingSchedules.orEmpty())
                    hasNextPage = pageData?.pageInfo?.hasNextPage == true
                    page++
                    emit(NetworkResult.Success(allSchedules.toCalendarFeed(now)))
                }
                is NetworkResult.Failure -> {
                    emit(NetworkResult.Failure(result.error))
                    return@flow
                }
            }
        }
    }
}
