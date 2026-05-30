package com.luum.michi.app.explore.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.search.data.SearchPage

internal interface ExploreRepository {
    suspend fun searchCatalog(
        query: String?,
        genre: String?,
        format: String?,
        year: Int?,
        sort: String,
        page: Int = 1,
        perPage: Int = 30,
        season: String? = null,
    ): NetworkResult<SearchPage>

    suspend fun searchManga(
        query: String?,
        genre: String?,
        format: String?,
        year: Int?,
        sort: String,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<SearchPage>

    suspend fun searchCharacters(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<SearchPage>

    suspend fun searchStaff(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<SearchPage>

    suspend fun searchStudios(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<SearchPage>
}
