package com.luum.michi.app.discover.data

import com.luum.michi.app.core.anilist.dto.CharacterDto
import com.luum.michi.app.core.anilist.dto.MediaPageInfoDto
import com.luum.michi.app.core.anilist.dto.MediaSearchResponseDto
import com.luum.michi.app.core.anilist.dto.StaffDto
import com.luum.michi.app.core.anilist.dto.StudioDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.platform.hexToPalette
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.data.toExploreResult
import com.luum.michi.app.discover.domain.model.ExplorePage
import com.luum.michi.app.discover.domain.model.ExploreResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

private const val AnimeCatalogQuery = """
query AnimeCatalog(
  ${'$'}search: String,
  ${'$'}format_in: [MediaFormat],
  ${'$'}genre_in: [String],
  ${'$'}seasonYear: Int,
  ${'$'}season: MediaSeason,
  ${'$'}sort: [MediaSort]!,
  ${'$'}page: Int!,
  ${'$'}perPage: Int!,
  ${'$'}onList: Boolean
) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    pageInfo { hasNextPage }
    media(
      search: ${'$'}search,
      type: ANIME,
      format_in: ${'$'}format_in,
      genre_in: ${'$'}genre_in,
      seasonYear: ${'$'}seasonYear,
      season: ${'$'}season,
      sort: ${'$'}sort,
      onList: ${'$'}onList,
      isAdult: false
    ) {
      id
      type
      title { romaji english native userPreferred }
      format
      status
      episodes
      chapters
      averageScore
      popularity
      favourites
      genres
      coverImage { extraLarge large medium color }
      season
      seasonYear
      startDate { year month day }
      isAdult
      isFavourite
      mediaListEntry { id score }
    }
  }
}
"""

private const val MangaCatalogQuery = """
query MangaCatalog(
  ${'$'}search: String,
  ${'$'}format_in: [MediaFormat],
  ${'$'}genre_in: [String],
  ${'$'}startDate_greater: FuzzyDateInt,
  ${'$'}startDate_lesser: FuzzyDateInt,
  ${'$'}sort: [MediaSort]!,
  ${'$'}page: Int!,
  ${'$'}perPage: Int!,
  ${'$'}onList: Boolean
) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    pageInfo { hasNextPage }
    media(
      search: ${'$'}search,
      type: MANGA,
      format_in: ${'$'}format_in,
      genre_in: ${'$'}genre_in,
      startDate_greater: ${'$'}startDate_greater,
      startDate_lesser: ${'$'}startDate_lesser,
      sort: ${'$'}sort,
      onList: ${'$'}onList,
      isAdult: false
    ) {
      id
      type
      title { romaji english native userPreferred }
      format
      status
      episodes
      chapters
      averageScore
      popularity
      favourites
      genres
      coverImage { extraLarge large medium color }
      season
      seasonYear
      startDate { year month day }
      isAdult
      isFavourite
      mediaListEntry { id score }
    }
  }
}
"""

private const val CharacterSearchQuery = """
query CharacterSearch(${'$'}search: String, ${'$'}sort: [CharacterSort], ${'$'}page: Int!, ${'$'}perPage: Int!) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    pageInfo { hasNextPage }
    characters(search: ${'$'}search, sort: ${'$'}sort) {
      id
      name { userPreferred full first last }
      image { large medium }
    }
  }
}
"""

private const val StaffSearchQuery = """
query StaffSearch(${'$'}search: String, ${'$'}sort: [StaffSort], ${'$'}page: Int!, ${'$'}perPage: Int!) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    pageInfo { hasNextPage }
    staff(search: ${'$'}search, sort: ${'$'}sort) {
      id
      name { userPreferred full first last }
      image { large medium }
    }
  }
}
"""

private const val StudioSearchQuery = """
query StudioSearch(${'$'}search: String, ${'$'}sort: [StudioSort], ${'$'}page: Int!, ${'$'}perPage: Int!) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    pageInfo { hasNextPage }
    studios(search: ${'$'}search, sort: ${'$'}sort) {
      id
      name
      media(sort: [START_DATE_DESC], perPage: 10) {
        nodes { status coverImage { extraLarge large medium color } }
      }
    }
  }
}
"""

internal class ExploreRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : ExploreRepository {

    override suspend fun searchCatalog(
        query: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
        sort: String,
        page: Int,
        perPage: Int,
        season: String?,
        onList: Boolean?,
        strings: LanguageStrings,
    ): NetworkResult<ExplorePage> {
        val variables = buildMap<String, JsonElement> {
            if (!query.isNullOrBlank()) {
                put("search", JsonPrimitive(query))
            }
            if (genres.isNotEmpty()) {
                put("genre_in", JsonArray(genres.map { JsonPrimitive(it) }))
            }
            if (formats.isNotEmpty()) {
                put("format_in", JsonArray(formats.map { JsonPrimitive(it.uppercase().replace(" ", "_")) }))
            }
            if (year != null && year > 0) {
                put("seasonYear", JsonPrimitive(year))
            }
            if (!season.isNullOrBlank()) {
                put("season", JsonPrimitive(season))
            }
            if (onList != null) {
                put("onList", JsonPrimitive(onList))
            }
            put("sort", JsonArray(listOf(JsonPrimitive(sort))))
            put("page", JsonPrimitive(page))
            put("perPage", JsonPrimitive(perPage))
        }

        val request = AniListGraphQLRequest(
            query = AnimeCatalogQuery,
            variables = JsonObject(variables),
            operationName = "AnimeCatalog",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaSearchResponseDto.serializer(), dataJson)
        }.map { response ->
            ExplorePage(
                results = response.page?.media.orEmpty().map { it.toExploreResult(strings) },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }

    override suspend fun searchManga(
        query: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
        sort: String,
        page: Int,
        perPage: Int,
        onList: Boolean?,
        strings: LanguageStrings,
    ): NetworkResult<ExplorePage> {
        val variables = buildMap<String, JsonElement> {
            if (!query.isNullOrBlank()) {
                put("search", JsonPrimitive(query))
            }
            if (genres.isNotEmpty()) {
                put("genre_in", JsonArray(genres.map { JsonPrimitive(it) }))
            }
            if (formats.isNotEmpty()) {
                put("format_in", JsonArray(formats.map { JsonPrimitive(it.uppercase().replace(" ", "_")) }))
            }
            if (year != null && year > 0) {
                val startYearGreater = (year - 1) * 10000 + 1231
                val startYearLesser = (year + 1) * 10000 + 101
                put("startDate_greater", JsonPrimitive(startYearGreater))
                put("startDate_lesser", JsonPrimitive(startYearLesser))
            }
            if (onList != null) {
                put("onList", JsonPrimitive(onList))
            }
            put("sort", JsonArray(listOf(JsonPrimitive(sort))))
            put("page", JsonPrimitive(page))
            put("perPage", JsonPrimitive(perPage))
        }

        val request = AniListGraphQLRequest(
            query = MangaCatalogQuery,
            variables = JsonObject(variables),
            operationName = "MangaCatalog",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaSearchResponseDto.serializer(), dataJson)
        }.map { response ->
            ExplorePage(
                results = response.page?.media.orEmpty().map { it.toExploreResult(strings) },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }

    override suspend fun searchCharacters(
        query: String?,
        page: Int,
        perPage: Int,
    ): NetworkResult<ExplorePage> {
        val variables = buildMap<String, JsonElement> {
            if (!query.isNullOrBlank()) {
                put("search", JsonPrimitive(query))
            } else {
                put("sort", JsonArray(listOf(JsonPrimitive("FAVOURITES_DESC"))))
            }
            put("page", JsonPrimitive(page))
            put("perPage", JsonPrimitive(perPage))
        }

        val request = AniListGraphQLRequest(
            query = CharacterSearchQuery,
            variables = JsonObject(variables),
            operationName = "CharacterSearch",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(CharacterSearchResponseDto.serializer(), dataJson)
        }.map { response ->
            ExplorePage(
                results = response.page?.characters.orEmpty().map { it.toExploreResult() },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }

    override suspend fun searchStaff(
        query: String?,
        page: Int,
        perPage: Int,
    ): NetworkResult<ExplorePage> {
        val variables = buildMap<String, JsonElement> {
            if (!query.isNullOrBlank()) {
                put("search", JsonPrimitive(query))
            } else {
                put("sort", JsonArray(listOf(JsonPrimitive("FAVOURITES_DESC"))))
            }
            put("page", JsonPrimitive(page))
            put("perPage", JsonPrimitive(perPage))
        }

        val request = AniListGraphQLRequest(
            query = StaffSearchQuery,
            variables = JsonObject(variables),
            operationName = "StaffSearch",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(StaffSearchResponseDto.serializer(), dataJson)
        }.map { response ->
            ExplorePage(
                results = response.page?.staff.orEmpty().map { it.toExploreResult() },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }

    override suspend fun searchStudios(
        query: String?,
        page: Int,
        perPage: Int,
    ): NetworkResult<ExplorePage> {
        val variables = buildMap<String, JsonElement> {
            if (!query.isNullOrBlank()) {
                put("search", JsonPrimitive(query))
            } else {
                put("sort", JsonArray(listOf(JsonPrimitive("FAVOURITES_DESC"))))
            }
            put("page", JsonPrimitive(page))
            put("perPage", JsonPrimitive(perPage))
        }

        val request = AniListGraphQLRequest(
            query = StudioSearchQuery,
            variables = JsonObject(variables),
            operationName = "StudioSearch",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(StudioSearchResponseDto.serializer(), dataJson)
        }.map { response ->
            ExplorePage(
                results = response.page?.studios.orEmpty().map { it.toExploreResult() },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }
}

private fun CharacterDto.toExploreResult(): ExploreResult = ExploreResult(
    id = id,
    title = name?.bestName ?: "",
    meta = "Character",
    coverUrl = image?.bestUrl,
    palette = hexToPalette(null),
    averageScore = null
)

private fun StaffDto.toExploreResult(): ExploreResult = ExploreResult(
    id = id,
    title = name?.bestName ?: "",
    meta = "Staff",
    coverUrl = image?.bestUrl,
    palette = hexToPalette(null),
    averageScore = null
)

private fun StudioDto.toExploreResult(): ExploreResult = ExploreResult(
    id = id,
    title = name,
    meta = "Studio",
    coverUrl = latestCoverImage?.thumbnailUrl,
    palette = hexToPalette(latestCoverImage?.color),
    averageScore = null
)

@Serializable
internal data class CharacterSearchResponseDto(
    @SerialName("Page") val page: CharacterExplorePageDto? = null,
)

@Serializable
internal data class CharacterExplorePageDto(
    val pageInfo: MediaPageInfoDto? = null,
    val characters: List<CharacterDto> = emptyList(),
)

@Serializable
internal data class StaffSearchResponseDto(
    @SerialName("Page") val page: StaffExplorePageDto? = null,
)

@Serializable
internal data class StaffExplorePageDto(
    val pageInfo: MediaPageInfoDto? = null,
    val staff: List<StaffDto> = emptyList(),
)

@Serializable
internal data class StudioSearchResponseDto(
    @SerialName("Page") val page: StudioExplorePageDto? = null,
)

@Serializable
internal data class StudioExplorePageDto(
    val pageInfo: MediaPageInfoDto? = null,
    val studios: List<StudioDto> = emptyList(),
)
