package com.luum.michi.app.account.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

import com.luum.michi.app.account.presentation.model.AccountFavorites
import com.luum.michi.app.account.presentation.model.AccountFavoritesCategory
import com.luum.michi.app.account.presentation.model.AccountStats
import com.luum.michi.app.core.anilist.dto.CharacterDto
import com.luum.michi.app.core.anilist.dto.MediaDto
import com.luum.michi.app.core.anilist.dto.StaffDto
import com.luum.michi.app.core.anilist.dto.StudioDto
import com.luum.michi.app.core.anilist.dto.UserAccountResponseDto
import com.luum.michi.app.core.anilist.dto.UserFavouritesResponseDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map

private const val UserAccountQuery = """
query UserAccount(${'$'}userId: Int!) {
  User(id: ${'$'}userId) {
    statistics {
      anime {
        count
        meanScore
        standardDeviation
        minutesWatched
        episodesWatched
        scores { score count meanScore }
        formats { format count }
        statuses { status count }
        genres(sort: COUNT_DESC, limit: 10) { genre count meanScore minutesWatched }
      }
      manga {
        count
        meanScore
        standardDeviation
        chaptersRead
        volumesRead
        scores { score count meanScore }
        formats { format count }
        statuses { status count }
        genres(sort: COUNT_DESC, limit: 10) { genre count meanScore minutesWatched }
      }
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

private fun favoritesPageQuery(category: AccountFavoritesCategory): String {
    val field = when (category) {
        AccountFavoritesCategory.ANIME -> """
            anime(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage }
              nodes {
                id
                title { romaji english native userPreferred }
                coverImage { extraLarge large medium color }
              }
            }
        """.trimIndent()
        AccountFavoritesCategory.MANGA -> """
            manga(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage }
              nodes {
                id
                title { romaji english native userPreferred }
                coverImage { extraLarge large medium color }
              }
            }
        """.trimIndent()
        AccountFavoritesCategory.CHARACTERS -> """
            characters(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage }
              nodes { id name { full userPreferred } image { large medium } }
            }
        """.trimIndent()
        AccountFavoritesCategory.STAFF -> """
            staff(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage }
              nodes { id name { full userPreferred } image { large medium } }
            }
        """.trimIndent()
        AccountFavoritesCategory.STUDIOS -> """
            studios(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage }
              nodes {
                id
                name
                media(sort: [START_DATE_DESC], perPage: 10) {
                  nodes { status coverImage { extraLarge large medium color } }
                }
              }
            }
        """.trimIndent()
    }
    return """
        query UserFavouritesPage(${'$'}userId: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!) {
          User(id: ${'$'}userId) {
            favourites {
              $field
            }
          }
        }
    """.trimIndent()
}

/** Items fetched per page in the "see more" favourites grid. */
private const val FavoritesPageSize = 25

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
                anime = response.user?.statistics?.anime.toAccountMediaTypeStats(isManga = false),
                manga = response.user?.statistics?.manga.toAccountMediaTypeStats(isManga = true),
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

    override suspend fun loadFavoritesPage(
        userId: Int,
        category: AccountFavoritesCategory,
        page: Int,
    ): NetworkResult<AccountFavoritesPage> {
        val request = AniListGraphQLRequest(
            query = favoritesPageQuery(category),
            variables = JsonObject(
                mapOf(
                    "userId" to JsonPrimitive(userId),
                    "page" to JsonPrimitive(page),
                    "perPage" to JsonPrimitive(FavoritesPageSize),
                ),
            ),
            operationName = "UserFavouritesPage",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(UserFavouritesResponseDto.serializer(), dataJson)
        }.map { response ->
            val favourites = response.user?.favourites
            when (category) {
                AccountFavoritesCategory.ANIME -> AccountFavoritesPage(
                    mediaItems = favourites?.anime?.nodes?.map(MediaDto::toAccountFavoriteMedia).orEmpty(),
                    hasNextPage = favourites?.anime?.pageInfo?.hasNextPage ?: false,
                )
                AccountFavoritesCategory.MANGA -> AccountFavoritesPage(
                    mediaItems = favourites?.manga?.nodes?.map(MediaDto::toAccountFavoriteMedia).orEmpty(),
                    hasNextPage = favourites?.manga?.pageInfo?.hasNextPage ?: false,
                )
                AccountFavoritesCategory.CHARACTERS -> AccountFavoritesPage(
                    personItems = favourites?.characters?.nodes?.map(CharacterDto::toAccountFavoritePerson).orEmpty(),
                    hasNextPage = favourites?.characters?.pageInfo?.hasNextPage ?: false,
                )
                AccountFavoritesCategory.STAFF -> AccountFavoritesPage(
                    personItems = favourites?.staff?.nodes?.map(StaffDto::toAccountFavoritePerson).orEmpty(),
                    hasNextPage = favourites?.staff?.pageInfo?.hasNextPage ?: false,
                )
                AccountFavoritesCategory.STUDIOS -> AccountFavoritesPage(
                    studioItems = favourites?.studios?.nodes?.map(StudioDto::toAccountFavoriteStudio).orEmpty(),
                    hasNextPage = favourites?.studios?.pageInfo?.hasNextPage ?: false,
                )
            }
        }
    }
}
