package com.luum.michi.app.profile.repository

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

import com.luum.michi.app.profile.domain.ProfileData
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.domain.model.ProfileFavorite
import com.luum.michi.app.profile.domain.model.ProfileFavorites
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.profile.domain.model.ProfileFavoritesCategory
import com.luum.michi.app.profile.domain.model.ProfileStats
import com.luum.michi.app.core.person.repository.dto.CharacterDto
import com.luum.michi.app.core.media.repository.dto.MediaDto
import com.luum.michi.app.core.person.repository.dto.StaffDto
import com.luum.michi.app.core.person.repository.dto.StudioDto
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.repository.dto.PageInfoDto
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

/** Shared node fields for studio favourites. Single source of truth for both
 *  profile queries. Studios have no image on AniList, so it is text only. */
private const val StudioFavoriteNodeFields = """
  id
  name
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
              pageInfo { hasNextPage currentPage }
              nodes {
                $MediaFavoriteNodeFields
              }
            }
        """.trimIndent()
        ProfileFavoritesCategory.MANGA -> """
            manga(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage currentPage }
              nodes {
                $MediaFavoriteNodeFields
              }
            }
        """.trimIndent()
        ProfileFavoritesCategory.CHARACTERS -> """
            characters(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage currentPage }
              nodes { $PersonFavoriteNodeFields }
            }
        """.trimIndent()
        ProfileFavoritesCategory.STAFF -> """
            staff(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage currentPage }
              nodes { $PersonFavoriteNodeFields }
            }
        """.trimIndent()
        ProfileFavoritesCategory.STUDIOS -> """
            studios(page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage currentPage }
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
    ): NetworkResult<Page<ProfileFavorite>> {
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
                ProfileFavoritesCategory.ANIME -> favoritePage(
                    nodes = favourites?.anime?.nodes,
                    pageInfo = favourites?.anime?.pageInfo,
                    requestedPage = page,
                    map = MediaDto::toProfileFavorite,
                )
                ProfileFavoritesCategory.MANGA -> favoritePage(
                    nodes = favourites?.manga?.nodes,
                    pageInfo = favourites?.manga?.pageInfo,
                    requestedPage = page,
                    map = MediaDto::toProfileFavorite,
                )
                ProfileFavoritesCategory.CHARACTERS -> favoritePage(
                    nodes = favourites?.characters?.nodes,
                    pageInfo = favourites?.characters?.pageInfo,
                    requestedPage = page,
                    map = CharacterDto::toProfileFavorite,
                )
                ProfileFavoritesCategory.STAFF -> favoritePage(
                    nodes = favourites?.staff?.nodes,
                    pageInfo = favourites?.staff?.pageInfo,
                    requestedPage = page,
                    map = StaffDto::toProfileFavorite,
                )
                ProfileFavoritesCategory.STUDIOS -> favoritePage(
                    nodes = favourites?.studios?.nodes,
                    pageInfo = favourites?.studios?.pageInfo,
                    requestedPage = page,
                    map = StudioDto::toProfileFavorite,
                )
            }
        }
    }
}

/** Maps one favourites connection to a [Page], dedup left to the holder's PagedList. */
private fun <T> favoritePage(
    nodes: List<T>?,
    pageInfo: PageInfoDto?,
    requestedPage: Int,
    map: (T) -> ProfileFavorite,
): Page<ProfileFavorite> = Page(
    items = nodes.orEmpty().map(map),
    hasNextPage = pageInfo?.hasNextPage ?: false,
    currentPage = pageInfo?.currentPage ?: requestedPage,
)
