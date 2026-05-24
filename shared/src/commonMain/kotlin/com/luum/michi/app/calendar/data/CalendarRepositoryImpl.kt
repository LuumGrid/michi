package com.luum.michi.app.calendar.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.luum.michi.app.core.anilist.dto.AiringScheduleDto
import com.luum.michi.app.core.anilist.dto.AiringSchedulePageDto
import com.luum.michi.app.core.auth.currentEpochSeconds
import com.luum.michi.app.core.media.currentSeasonAndYear
import com.luum.michi.app.core.media.next
import com.luum.michi.app.core.media.startEpochSeconds
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult

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
        episodes
        averageScore
        favourites
        popularity
        title { romaji english native userPreferred }
        coverImage { extraLarge large medium color }
        isFavourite
        mediaListEntry { id status progress score }
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
                AniListJson.decodeFromString(CalendarResponseDto.serializer(), dataJson)
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
