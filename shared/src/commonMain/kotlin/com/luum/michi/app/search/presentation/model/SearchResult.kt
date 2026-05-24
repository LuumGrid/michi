package com.luum.michi.app.search.presentation.model

import androidx.compose.ui.graphics.Color

internal data class SearchResult(
    val id: Int,
    val title: String,
    val meta: String,
    val coverUrl: String?,
    val palette: List<Color>,
    val averageScore: Int?,
    val favourites: Int? = null,
    val genres: List<String> = emptyList(),
    val isUserFavorited: Boolean = false,
    val isUserRanked: Boolean = false,
)
