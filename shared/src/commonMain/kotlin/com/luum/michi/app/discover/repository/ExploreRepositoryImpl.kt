package com.luum.michi.app.discover.repository

import com.luum.michi.app.core.model.MediaGridItem
import com.luum.michi.app.core.model.MediaType
import com.luum.michi.app.core.model.Page
import com.luum.michi.app.core.network.repository.dto.CharacterDto
import com.luum.michi.app.core.network.repository.dto.PageInfoDto
import com.luum.michi.app.core.network.repository.dto.MediaSearchResponseDto
import com.luum.michi.app.core.network.repository.dto.StaffDto
import com.luum.michi.app.core.network.repository.dto.StudioDto
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.GridPageSize
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.domain.map
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.model.ExplorePage
import com.luum.michi.app.discover.domain.model.ExploreResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.luum.michi.app.core.network.repository.toCoverSizes
import com.luum.michi.app.core.network.repository.toMediaGridItem

/**
 * One catalog query for both types: `$type` decides the API `type` and the year
 * filter (`season`/`seasonYear` for anime, `startDate_greater`/`lesser` for
 * manga). `mediaListEntry` carries `status` so the shared grid can read the
 * viewer's list status.
 */
private const val MediaCatalogQuery = """
query MediaCatalog(
  ${'$'}type: MediaType,
  ${'$'}search: String,
  ${'$'}format_in: [MediaFormat],
  ${'$'}genre_in: [String],
  ${'$'}seasonYear: Int,
  ${'$'}season: MediaSeason,
  ${'$'}startDate_greater: FuzzyDateInt,
  ${'$'}startDate_lesser: FuzzyDateInt,
  ${'$'}sort: [MediaSort]!,
  ${'$'}page: Int!,
  ${'$'}perPage: Int!,
  ${'$'}onList: Boolean,
  ${'$'}isAdult: Boolean
) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    pageInfo { hasNextPage currentPage }
    media(
      search: ${'$'}search,
      type: ${'$'}type,
      format_in: ${'$'}format_in,
      genre_in: ${'$'}genre_in,
      seasonYear: ${'$'}seasonYear,
      season: ${'$'}season,
      startDate_greater: ${'$'}startDate_greater,
      startDate_lesser: ${'$'}startDate_lesser,
      sort: ${'$'}sort,
      onList: ${'$'}onList,
      isAdult: ${'$'}isAdult
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
      mediaListEntry { id score status }
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

    override suspend fun searchMedia(
        type: MediaType,
        query: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
        sort: String,
        page: Int,
        perPage: Int,
        season: String?,
        onList: Boolean?,
        hideAdult: Boolean,
        strings: LanguageStrings,
    ): NetworkResult<ExplorePage> {
        val request = AniListGraphQLRequest(
            query = MediaCatalogQuery,
            variables = JsonObject(
                buildCatalogVariables(
                    type = type,
                    query = query,
                    genres = genres,
                    formats = formats,
                    year = year,
                    season = season,
                    sort = sort,
                    page = page,
                    perPage = perPage,
                    onList = onList,
                    hideAdult = hideAdult,
                ),
            ),
            operationName = "MediaCatalog",
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

    override suspend fun searchMediaGrid(
        type: MediaType,
        genres: List<String>,
        page: Int,
        sort: String,
        hideAdult: Boolean,
    ): NetworkResult<Page<MediaGridItem>> {
        val request = AniListGraphQLRequest(
            query = MediaCatalogQuery,
            variables = JsonObject(
                buildCatalogVariables(
                    type = type,
                    query = null,
                    genres = genres,
                    formats = emptyList(),
                    year = null,
                    season = null,
                    sort = sort,
                    page = page,
                    perPage = GridPageSize,
                    onList = null,
                    hideAdult = hideAdult,
                ),
            ),
            operationName = "MediaCatalog",
        )

        return graphQLClient.execute(request) { dataJson ->
            AniListJson.decodeFromJsonElement(MediaSearchResponseDto.serializer(), dataJson)
        }.map { response ->
            Page(
                items = response.page?.media.orEmpty().map { it.toMediaGridItem() },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
                currentPage = response.page?.pageInfo?.currentPage ?: page,
            )
        }
    }

    override suspend fun searchCharacters(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
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
                results = response.page?.characters.orEmpty().map { it.toExploreResult(strings) },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }

    override suspend fun searchStaff(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
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
                results = response.page?.staff.orEmpty().map { it.toExploreResult(strings) },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }

    override suspend fun searchStudios(
        query: String?,
        page: Int,
        perPage: Int,
        strings: LanguageStrings,
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
                results = response.page?.studios.orEmpty().map { it.toExploreResult(strings) },
                hasNextPage = response.page?.pageInfo?.hasNextPage == true,
            )
        }
    }
}

/**
 * Catalog variables shared by [ExploreRepositoryImpl.searchMedia] and
 * [ExploreRepositoryImpl.searchMediaGrid]. The year filter follows the type:
 * anime uses `season`/`seasonYear`, manga uses `startDate_greater`/`lesser`.
 */
private fun buildCatalogVariables(
    type: MediaType,
    query: String?,
    genres: List<String>,
    formats: List<String>,
    year: Int?,
    season: String?,
    sort: String,
    page: Int,
    perPage: Int,
    onList: Boolean?,
    hideAdult: Boolean,
): Map<String, JsonElement> = buildMap {
    put("type", JsonPrimitive(type.name))
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
        if (type == MediaType.MANGA) {
            put("startDate_greater", JsonPrimitive((year - 1) * 10000 + 1231))
            put("startDate_lesser", JsonPrimitive((year + 1) * 10000 + 101))
        } else {
            put("seasonYear", JsonPrimitive(year))
        }
    }
    if (type == MediaType.ANIME && !season.isNullOrBlank()) {
        put("season", JsonPrimitive(season))
    }
    if (onList != null) {
        put("onList", JsonPrimitive(onList))
    }
    // Omit (null) when adult is allowed: the server then returns both.
    // Never send true — that would filter to adult ONLY.
    if (hideAdult) {
        put("isAdult", JsonPrimitive(false))
    }
    put("sort", JsonArray(listOf(JsonPrimitive(sort))))
    put("page", JsonPrimitive(page))
    put("perPage", JsonPrimitive(perPage))
}

private fun CharacterDto.toExploreResult(strings: LanguageStrings): ExploreResult = ExploreResult(
    id = id,
    title = name?.bestName ?: "",
    meta = strings.characterLabel,
    cover = image?.toCoverSizes(),
    paletteHex = null,
    averageScore = null
)

private fun StaffDto.toExploreResult(strings: LanguageStrings): ExploreResult = ExploreResult(
    id = id,
    title = name?.bestName ?: "",
    meta = strings.staffLabel,
    cover = image?.toCoverSizes(),
    paletteHex = null,
    averageScore = null
)

private fun StudioDto.toExploreResult(strings: LanguageStrings): ExploreResult = ExploreResult(
    id = id,
    title = name,
    meta = strings.studioLabel,
    cover = latestCoverImage?.toCoverSizes(),
    paletteHex = latestCoverImage?.color,
    averageScore = null
)

@Serializable
internal data class CharacterSearchResponseDto(
    @SerialName("Page") val page: CharacterExplorePageDto? = null,
)

@Serializable
internal data class CharacterExplorePageDto(
    val pageInfo: PageInfoDto? = null,
    val characters: List<CharacterDto> = emptyList(),
)

@Serializable
internal data class StaffSearchResponseDto(
    @SerialName("Page") val page: StaffExplorePageDto? = null,
)

@Serializable
internal data class StaffExplorePageDto(
    val pageInfo: PageInfoDto? = null,
    val staff: List<StaffDto> = emptyList(),
)

@Serializable
internal data class StudioSearchResponseDto(
    @SerialName("Page") val page: StudioExplorePageDto? = null,
)

@Serializable
internal data class StudioExplorePageDto(
    val pageInfo: PageInfoDto? = null,
    val studios: List<StudioDto> = emptyList(),
)
