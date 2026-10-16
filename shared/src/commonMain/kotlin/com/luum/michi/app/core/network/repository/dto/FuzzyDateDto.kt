package com.luum.michi.app.core.network.repository.dto

import kotlinx.serialization.Serializable

/** AniList `FuzzyDate` shape (year/month/day all optional). */
@Serializable
internal data class FuzzyDateDto(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
)
