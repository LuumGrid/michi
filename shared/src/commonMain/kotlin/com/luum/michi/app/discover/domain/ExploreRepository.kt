package com.luum.michi.app.discover.domain

import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.discover.domain.model.ExplorePage

internal interface ExploreRepository {
    suspend fun searchCatalog(
        query: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
        sort: String,
        page: Int = 1,
        perPage: Int = 30,
        season: String? = null,
        onList: Boolean? = null,
        strings: LanguageStrings,
    ): NetworkResult<ExplorePage>

    suspend fun searchManga(
        query: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
        sort: String,
        page: Int = 1,
        perPage: Int = 30,
        onList: Boolean? = null,
        strings: LanguageStrings,
    ): NetworkResult<ExplorePage>

    suspend fun searchCharacters(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<ExplorePage>

    suspend fun searchStaff(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<ExplorePage>

    suspend fun searchStudios(
        query: String?,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<ExplorePage>
}
