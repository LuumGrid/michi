package com.luum.michi.app.core.model

/**
 * One page of a connection plus its pagination cursor. Shared by every paged
 * list (media characters/staff/recommendations, character/staff/studio works,
 * genre works, explore) so the repositories and holders agree on the shape.
 */
internal data class Page<T>(
    val items: List<T>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)
