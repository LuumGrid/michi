package com.luum.michi.app.explore.data

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
import com.luum.michi.app.core.platform.hexToPalette
import com.luum.michi.app.search.data.SearchPage
import com.luum.michi.app.search.data.toSearchResult
import com.luum.michi.app.search.presentation.model.SearchResult
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
  ${'$'}format: MediaFormat,
  ${'$'}genre: String,
  ${'$'}seasonYear: Int,
  ${'$'}sort: [MediaSort]!,
  ${'$'}page: Int!,
  ${'$'}perPage: Int!
) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    pageInfo { hasNextPage }
    media(
      search: ${'$'}search,
      type: ANIME,
      format: ${'$'}format,
      genre: ${'$'}genre,
      seasonYear: ${'$'}seasonYear,
      sort: ${'$'}sort,
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
  ${'$'}format: MediaFormat,
  ${'$'}genre: String,
  ${'$'}startDate_greater: FuzzyDateInt,
  ${'$'}startDate_lesser: FuzzyDateInt,
  ${'$'}sort: [MediaSort]!,
  ${'$'}page: Int!,
  ${'$'}perPage: Int!
) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    pageInfo { hasNextPage }
    media(
      search: ${'$'}search,
      type: MANGA,
      format: ${'$'}format,
      genre: ${'$'}genre,
      startDate_greater: ${'$'}startDate_greater,
      startDate_lesser: ${'$'}startDate_lesser,
      sort: ${'$'}sort,
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
        genre: String?,
        format: String?,
        year: Int?,
        sort: String,
        page: Int,
        perPage: Int,
    ): NetworkResult<SearchPage> {
        val variables = buildMap<String, JsonElement> {
            if (!query.isNullOrBlank()) {
                put("search", JsonPrimitive(query))
            }
            if (!genre.isNullOrBlank() && genre != "All" && genre != "Todos") {
                put("genre", JsonPrimitive(genre))
            }
            if (!format.isNullOrBlank() && format != "All" && format != "Todos") {
                put("format", JsonPrimitive(format.uppercase().replace(" ", "_")))
            }
            if (year != null && year > 0) {
                put("seasonYear", JsonPrimitive(year))
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
            SearchPage(
                results = response.page?.media.orEmpty().map { it.toSearchResult() },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }

    override suspend fun searchManga(
        query: String?,
        genre: String?,
        format: String?,
        year: Int?,
        sort: String,
        page: Int,
        perPage: Int,
    ): NetworkResult<SearchPage> {
        val variables = buildMap<String, JsonElement> {
            if (!query.isNullOrBlank()) {
                put("search", JsonPrimitive(query))
            }
            if (!genre.isNullOrBlank() && genre != "All" && genre != "Todos") {
                put("genre", JsonPrimitive(genre))
            }
            if (!format.isNullOrBlank() && format != "All" && format != "Todos") {
                put("format", JsonPrimitive(format.uppercase().replace(" ", "_")))
            }
            if (year != null && year > 0) {
                val startYearGreater = (year - 1) * 10000 + 1231
                val startYearLesser = (year + 1) * 10000 + 101
                put("startDate_greater", JsonPrimitive(startYearGreater))
                put("startDate_lesser", JsonPrimitive(startYearLesser))
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
            SearchPage(
                results = response.page?.media.orEmpty().map { it.toSearchResult() },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }

    override suspend fun searchCharacters(
        query: String?,
        page: Int,
        perPage: Int,
    ): NetworkResult<SearchPage> {
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
            SearchPage(
                results = response.page?.characters.orEmpty().map { it.toSearchResult() },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }

    override suspend fun searchStaff(
        query: String?,
        page: Int,
        perPage: Int,
    ): NetworkResult<SearchPage> {
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
            SearchPage(
                results = response.page?.staff.orEmpty().map { it.toSearchResult() },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }

    override suspend fun searchStudios(
        query: String?,
        page: Int,
        perPage: Int,
    ): NetworkResult<SearchPage> {
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
            SearchPage(
                results = response.page?.studios.orEmpty().map { it.toSearchResult() },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }
}

private fun CharacterDto.toSearchResult(): SearchResult = SearchResult(
    id = id,
    title = name?.bestName ?: "",
    meta = "Character",
    coverUrl = image?.bestUrl,
    palette = hexToPalette(null),
    averageScore = null
)

private fun StaffDto.toSearchResult(): SearchResult = SearchResult(
    id = id,
    title = name?.bestName ?: "",
    meta = "Staff",
    coverUrl = image?.bestUrl,
    palette = hexToPalette(null),
    averageScore = null
)

private fun StudioDto.toSearchResult(): SearchResult = SearchResult(
    id = id,
    title = name,
    meta = "Studio",
    coverUrl = latestCoverImage?.thumbnailUrl,
    palette = hexToPalette(latestCoverImage?.color),
    averageScore = null
)

@Serializable
internal data class CharacterSearchResponseDto(
    @SerialName("Page") val page: CharacterSearchPageDto? = null,
)

@Serializable
internal data class CharacterSearchPageDto(
    val pageInfo: MediaPageInfoDto? = null,
    val characters: List<CharacterDto> = emptyList(),
)

@Serializable
internal data class StaffSearchResponseDto(
    @SerialName("Page") val page: StaffSearchPageDto? = null,
)

@Serializable
internal data class StaffSearchPageDto(
    val pageInfo: MediaPageInfoDto? = null,
    val staff: List<StaffDto> = emptyList(),
)

@Serializable
internal data class StudioSearchResponseDto(
    @SerialName("Page") val page: StudioSearchPageDto? = null,
)

@Serializable
internal data class StudioSearchPageDto(
    val pageInfo: MediaPageInfoDto? = null,
    val studios: List<StudioDto> = emptyList(),
)
