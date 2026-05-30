package com.luum.michi.app.search.data

import com.luum.michi.app.search.presentation.model.SearchResult

internal data class SearchPage(
    val results: List<SearchResult>,
    val hasNextPage: Boolean,
)
