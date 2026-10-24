package com.luum.michi.app.core.network.repository.dto

import kotlinx.serialization.Serializable

/**
 * AniList `PageInfo` shape, shared by every paged query. Every field defaults so
 * a query that only asks for `hasNextPage` (or a subset) decodes the rest as null.
 */
@Serializable
internal data class PageInfoDto(
    val hasNextPage: Boolean = false,
    val currentPage: Int? = null,
    val total: Int? = null,
    val lastPage: Int? = null,
    val perPage: Int? = null,
)
