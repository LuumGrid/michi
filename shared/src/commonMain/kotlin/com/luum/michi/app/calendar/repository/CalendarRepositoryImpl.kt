package com.luum.michi.app.calendar.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.luum.michi.app.calendar.domain.CalendarEntry
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.core.network.repository.dto.AiringScheduleDto
import com.luum.michi.app.core.network.repository.dto.AiringSchedulePageDto
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
        status
        season
        seasonYear
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
) : CalendarRepository {

    override suspend fun loadDay(from: Long, to: Long): NetworkResult<List<CalendarEntry>> {
        val allSchedules = mutableListOf<AiringScheduleDto>()
        var page = 1
        var hasNextPage = true

        while (hasNextPage) {
            val request = AniListGraphQLRequest(
                query = CalendarQuery,
                variables = JsonObject(
                    mapOf(
                        "from" to JsonPrimitive(from),
                        "to" to JsonPrimitive(to),
                        "page" to JsonPrimitive(page),
                    ),
                ),
                operationName = "Calendar",
            )
            when (
                val result = graphQLClient.execute(request) { dataJson ->
                    AniListJson.decodeFromJsonElement(CalendarResponseDto.serializer(), dataJson)
                }
            ) {
                is NetworkResult.Success -> {
                    val pageData = result.value.page
                    allSchedules.addAll(pageData?.airingSchedules.orEmpty())
                    hasNextPage = pageData?.pageInfo?.hasNextPage == true
                    page++
                }
                is NetworkResult.Failure -> return NetworkResult.Failure(result.error)
            }
        }
        return NetworkResult.Success(
            allSchedules.mapNotNull { schedule ->
                schedule.toReleaseItemOrNull()?.let { item ->
                    CalendarEntry(scheduleId = schedule.id, item = item)
                }
            },
        )
    }
}
