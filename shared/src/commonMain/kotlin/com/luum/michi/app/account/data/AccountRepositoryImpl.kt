package com.luum.michi.app.account.data

import com.luum.michi.app.account.presentation.model.AccountFavorites
import com.luum.michi.app.account.presentation.model.AccountStats
import com.luum.michi.app.core.anilist.dto.UserAccountResponseDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

private const val UserAccountQuery = """
query UserAccount(${'$'}userId: Int!) {
  User(id: ${'$'}userId) {
    statistics {
      anime { count meanScore minutesWatched }
      manga { count meanScore chaptersRead volumesRead }
    }
    favourites {
      anime(perPage: 12) {
        nodes {
          id
          title { romaji english native userPreferred }
          coverImage { extraLarge large medium color }
        }
      }
      manga(perPage: 12) {
        nodes {
          id
          title { romaji english native userPreferred }
          coverImage { extraLarge large medium color }
        }
      }
      characters(perPage: 12) {
        nodes {
          id
          name { full userPreferred }
          image { large medium }
        }
      }
      staff(perPage: 12) {
        nodes {
          id
          name { full userPreferred }
          image { large medium }
        }
      }
      studios(perPage: 12) {
        nodes {
          id
          name
          media(sort: [START_DATE_DESC], perPage: 10) {
            nodes { status coverImage { extraLarge large medium color } }
          }
        }
      }
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

internal class AccountRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : AccountRepository {

    override suspend fun loadAccount(userId: Int): NetworkResult<AccountData> {
        val request = AniListGraphQLRequest(
            query = UserAccountQuery,
            variables = JsonObject(mapOf("userId" to JsonPrimitive(userId))),
            operationName = "UserAccount",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(UserAccountResponseDto.serializer(), dataJson)
        }.map { response ->
            val stats = AccountStats(
                animeCount = response.user?.statistics?.anime?.count ?: 0,
                mangaCount = response.user?.statistics?.manga?.count ?: 0,
                followingCount = response.following?.pageInfo?.total ?: 0,
                followersCount = response.followers?.pageInfo?.total ?: 0,
            )
            val favorites = response.user?.favourites?.toDomain()
                ?: AccountFavorites(
                    anime = emptyList(),
                    manga = emptyList(),
                    characters = emptyList(),
                    staff = emptyList(),
                    studios = emptyList(),
                )
            AccountData(stats = stats, favorites = favorites)
        }
    }
}
