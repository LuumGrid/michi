package com.luum.michi.app.account.data

import com.luum.michi.app.account.presentation.model.AccountStats
import com.luum.michi.app.core.anilist.dto.UserStatsResponseDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

private const val UserStatsQuery = """
query UserStats(${'$'}userId: Int!) {
  User(id: ${'$'}userId) {
    statistics {
      anime { count meanScore minutesWatched }
      manga { count meanScore chaptersRead volumesRead }
    }
  }
  followers: Page(perPage: 1) {
    pageInfo { total }
    followers(userId: ${'$'}userId) { id }
  }
  following: Page(perPage: 1) {
    pageInfo { total }
    following(userId: ${'$'}userId) { id }
  }
}
"""

internal class AccountStatsRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : AccountStatsRepository {

    override suspend fun loadStats(userId: Int): NetworkResult<AccountStats> {
        val request = AniListGraphQLRequest(
            query = UserStatsQuery,
            variables = JsonObject(mapOf("userId" to JsonPrimitive(userId))),
            operationName = "UserStats",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(UserStatsResponseDto.serializer(), dataJson)
        }.map { response ->
            AccountStats(
                animeCount = response.user?.statistics?.anime?.count ?: 0,
                mangaCount = response.user?.statistics?.manga?.count ?: 0,
                followingCount = response.following?.pageInfo?.total ?: 0,
                followersCount = response.followers?.pageInfo?.total ?: 0,
            )
        }
    }
}
