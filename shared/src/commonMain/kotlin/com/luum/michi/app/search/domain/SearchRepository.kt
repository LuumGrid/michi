package com.luum.michi.app.search.domain

import com.luum.michi.app.search.domain.model.SearchResult

internal data class SearchPage(
    val results: List<SearchResult>,
    val hasNextPage: Boolean,
)
