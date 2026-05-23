package com.luum.michi.app.account.presentation.model

import androidx.compose.ui.graphics.Color

internal data class AccountFavoriteMedia(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val palette: List<Color>,
)

internal data class AccountFavoritePerson(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val palette: List<Color>,
)

internal data class AccountFavoriteStudio(
    val id: Int,
    val name: String,
    val palette: List<Color>,
)

internal data class AccountFavorites(
    val anime: List<AccountFavoriteMedia>,
    val manga: List<AccountFavoriteMedia>,
    val characters: List<AccountFavoritePerson>,
    val staff: List<AccountFavoritePerson>,
    val studios: List<AccountFavoriteStudio>,
)
