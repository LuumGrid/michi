package com.luum.michi.app.explore.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.search.presentation.model.SearchResult

internal interface ExploreRepository {
    suspend fun searchCatalog(
        query: String?,
        genre: String?,
        format: String?,
        year: Int?,
        sort: String,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<List<SearchResult>>

    suspend fun searchManga(
        query: String?,
        genre: String?,
        format: String?,
        year: Int?,
        sort: String,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<List<SearchResult>>

    suspend fun searchCharacters(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<List<SearchResult>>

    suspend fun searchStaff(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<List<SearchResult>>

    suspend fun searchStudios(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<List<SearchResult>>
}
