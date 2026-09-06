package com.luum.michi.app.discover.domain.model

internal data class ExplorePage(
    val results: List<ExploreResult>,
    val hasNextPage: Boolean,
)
