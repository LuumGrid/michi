package com.luum.michi.app.profile.repository

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

import com.luum.michi.app.profile.domain.ProfileData
import com.luum.michi.app.profile.domain.ProfileFavoritesPage
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.domain.model.ProfileStats
import com.luum.michi.app.core.network.repository.dto.CharacterDto
import com.luum.michi.app.core.network.repository.dto.MediaDto
import com.luum.michi.app.core.network.repository.dto.StaffDto
import com.luum.michi.app.core.network.repository.dto.StudioDto
import com.luum.michi.app.core.network.repository.dto.UserProfileResponseDto
import com.luum.michi.app.core.network.repository.dto.UserFavouritesResponseDto
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map

/** Shared node fields for anime/manga favourites. Single source of truth for both profile queries. */
private const val MediaFavoriteNodeFields = """
  id
  title { romaji english native userPreferred }
  coverImage { extraLarge large medium color }
"""

/** Shared node fields for character/staff favourites. Single source of truth for both profile queries. */
private const val PersonFavoriteNodeFields = """
  id
  name { full userPreferred }
  image { large medium }
"""

/** Shared node fields for studio favourites. Single source of truth for both profile queries. */
private const val StudioFavoriteNodeFields = """
  id
  name
  media(sort: [START_DATE_DESC], perPage: 10) {
    nodes { coverImage { extraLarge large medium color } }
  }
"""

private const val UserProfileQuery = """
query UserProfile(${'$'}userId: Int!) {
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
        genres(sort: COUNT_DESC, limit: 10) { genre count meanScore }
      }
    }
    favourites {
      anime(perPage: 12) {
        nodes {
          $MediaFavoriteNodeFields
        }
      }
      manga(perPage: 12) {
        nodes {
          $MediaFavoriteNodeFields
        }
      }
      characters(perPage: 12) {
        nodes {
          $PersonFavoriteNodeFields
        }
      }
      staff(perPage: 12) {
        nodes {
          $PersonFavoriteNodeFields
        }
      }
      studios(perPage: 12) {
        nodes {
          $StudioFavoriteNodeFields
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

private fun favoritesPageQuery(category: ProfileFavoritesCategory): String {
    val field = when (category) {
        ProfileFavoritesCategory.ANIME -> """
            anime(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage }
              nodes {
                $MediaFavoriteNodeFields
              }
            }
        """.trimIndent()
        ProfileFavoritesCategory.MANGA -> """
            manga(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage }
              nodes {
                $MediaFavoriteNodeFields
              }
            }
        """.trimIndent()
        ProfileFavoritesCategory.CHARACTERS -> """
            characters(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage }
              nodes { $PersonFavoriteNodeFields }
            }
        """.trimIndent()
        ProfileFavoritesCategory.STAFF -> """
            staff(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage }
              nodes { $PersonFavoriteNodeFields }
            }
        """.trimIndent()
        ProfileFavoritesCategory.STUDIOS -> """
            studios(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage }
              nodes {
                $StudioFavoriteNodeFields
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

internal class ProfileRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : ProfileRepository {

    override suspend fun loadProfile(userId: Int): NetworkResult<ProfileData> {
        val request = AniListGraphQLRequest(
            query = UserProfileQuery,
            variables = JsonObject(mapOf("userId" to JsonPrimitive(userId))),
            operationName = "UserProfile",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(UserProfileResponseDto.serializer(), dataJson)
        }.map { response ->
            val stats = ProfileStats(
                animeCount = response.user?.statistics?.anime?.count ?: 0,
                mangaCount = response.user?.statistics?.manga?.count ?: 0,
                followingCount = response.following?.pageInfo?.total ?: 0,
                followersCount = response.followers?.pageInfo?.total ?: 0,
                anime = response.user?.statistics?.anime.toProfileMediaTypeStats(isManga = false),
                manga = response.user?.statistics?.manga.toProfileMediaTypeStats(isManga = true),
            )
            val favorites = response.user?.favourites?.toDomain() ?: ProfileFavorites.EMPTY
            ProfileData(stats = stats, favorites = favorites)
        }
    }

    override suspend fun loadFavoritesPage(
        userId: Int,
        category: ProfileFavoritesCategory,
        page: Int,
    ): NetworkResult<ProfileFavoritesPage> {
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
                ProfileFavoritesCategory.ANIME -> ProfileFavoritesPage(
                    mediaItems = favourites?.anime?.nodes?.map(MediaDto::toProfileFavoriteMedia).orEmpty(),
                    hasNextPage = favourites?.anime?.pageInfo?.hasNextPage ?: false,
                )
                ProfileFavoritesCategory.MANGA -> ProfileFavoritesPage(
                    mediaItems = favourites?.manga?.nodes?.map(MediaDto::toProfileFavoriteMedia).orEmpty(),
                    hasNextPage = favourites?.manga?.pageInfo?.hasNextPage ?: false,
                )
                ProfileFavoritesCategory.CHARACTERS -> ProfileFavoritesPage(
                    personItems = favourites?.characters?.nodes?.map(CharacterDto::toProfileFavoritePerson).orEmpty(),
                    hasNextPage = favourites?.characters?.pageInfo?.hasNextPage ?: false,
                )
                ProfileFavoritesCategory.STAFF -> ProfileFavoritesPage(
                    personItems = favourites?.staff?.nodes?.map(StaffDto::toProfileFavoritePerson).orEmpty(),
                    hasNextPage = favourites?.staff?.pageInfo?.hasNextPage ?: false,
                )
                ProfileFavoritesCategory.STUDIOS -> ProfileFavoritesPage(
                    studioItems = favourites?.studios?.nodes?.map(StudioDto::toProfileFavoriteStudio).orEmpty(),
                    hasNextPage = favourites?.studios?.pageInfo?.hasNextPage ?: false,
                )
            }
        }
    }
}
