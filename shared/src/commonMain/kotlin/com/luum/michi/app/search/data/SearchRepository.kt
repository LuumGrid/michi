package com.luum.michi.app.search.data

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.search.presentation.model.SearchResult
import com.luum.michi.app.search.presentation.model.SearchType

internal data class SearchPage(
    val results: List<SearchResult>,
    val hasNextPage: Boolean,
)

internal interface SearchRepository {
    suspend fun search(
        query: String,
        type: SearchType,
        page: Int = 1,
        perPage: Int = 30,
    ): NetworkResult<SearchPage>
}
