package com.luum.michi.app.calendar.data

import com.luum.michi.app.core.anilist.dto.AiringSchedulePageDto
import com.luum.michi.app.core.auth.currentEpochSeconds
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val SecondsPerDay: Long = 86_400L
private const val CalendarDays: Int = 7

private const val CalendarQuery = """
query Calendar(${'$'}from: Int!, ${'$'}to: Int!) {
  Page(perPage: 50) {
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

@Serializable
private data class CalendarResponseDto(
    @SerialName("Page") val page: AiringSchedulePageDto? = null,
)

internal class CalendarRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val nowProvider: () -> Long = { currentEpochSeconds() },
) : CalendarRepository {

    override suspend fun loadFeed(): NetworkResult<CalendarFeed> {
        val now = nowProvider()
        val to = now + SecondsPerDay * CalendarDays

        val request = AniListGraphQLRequest(
            query = CalendarQuery,
            variables = JsonObject(
                mapOf(
                    "from" to JsonPrimitive(now),
                    "to" to JsonPrimitive(to),
                ),
            ),
            operationName = "Calendar",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromString(CalendarResponseDto.serializer(), dataJson)
        }.map { response ->
            response.page?.toCalendarFeed(now) ?: CalendarFeed(days = emptyList())
        }
    }
}
