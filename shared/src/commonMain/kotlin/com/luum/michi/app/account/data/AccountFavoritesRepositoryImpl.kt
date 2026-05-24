package com.luum.michi.app.account.data

import com.luum.michi.app.account.presentation.model.AccountFavorites
import com.luum.michi.app.core.anilist.dto.UserFavouritesResponseDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val UserFavouritesQuery = """
query UserFavourites(${'$'}userId: Int!) {
  User(id: ${'$'}userId) {
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
}
"""

internal class AccountFavoritesRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : AccountFavoritesRepository {

    override suspend fun loadFavorites(userId: Int): NetworkResult<AccountFavorites> {
        val request = AniListGraphQLRequest(
            query = UserFavouritesQuery,
            variables = JsonObject(mapOf("userId" to JsonPrimitive(userId))),
            operationName = "UserFavourites",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromString(UserFavouritesResponseDto.serializer(), dataJson)
        }.map { response ->
            response.user?.favourites?.toDomain() ?: AccountFavorites(
                anime = emptyList(),
                manga = emptyList(),
                characters = emptyList(),
                staff = emptyList(),
                studios = emptyList(),
            )
        }
    }
}
