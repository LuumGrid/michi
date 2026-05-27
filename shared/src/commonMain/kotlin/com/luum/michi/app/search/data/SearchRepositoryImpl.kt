package com.luum.michi.app.search.data

import com.luum.michi.app.core.anilist.dto.MediaSearchResponseDto
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.AniListGraphQLRequest
import com.luum.michi.app.core.network.AniListJson
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.network.map
import com.luum.michi.app.search.presentation.model.SearchResult
import com.luum.michi.app.search.presentation.model.SearchType
import com.luum.michi.app.search.presentation.model.toApiValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

private const val MediaSearchQuery = """
query MediaSearch(${'$'}search: String, ${'$'}type: MediaType, ${'$'}page: Int!, ${'$'}perPage: Int!) {
  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
    pageInfo { hasNextPage }
    media(search: ${'$'}search, type: ${'$'}type, sort: SEARCH_MATCH, isAdult: false) {
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

internal class SearchRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
) : SearchRepository {

    override suspend fun search(
        query: String,
        type: SearchType,
        page: Int,
        perPage: Int,
    ): NetworkResult<SearchPage> {
        val variables = buildMap<String, JsonElement> {
            put("search", JsonPrimitive(query))
            type.toApiValue()?.let { put("type", JsonPrimitive(it)) }
            put("page", JsonPrimitive(page))
            put("perPage", JsonPrimitive(perPage))
        }
        val request = AniListGraphQLRequest(
            query = MediaSearchQuery,
            variables = JsonObject(variables),
            operationName = "MediaSearch",
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
}
