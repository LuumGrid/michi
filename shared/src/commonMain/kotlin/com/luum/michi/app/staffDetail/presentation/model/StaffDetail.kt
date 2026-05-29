package com.luum.michi.app.staffDetail.presentation.model

import androidx.compose.ui.graphics.Color

internal data class StaffDetail(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val alternativeNames: List<String>,
    val imageUrl: String?,
    val descriptionPlain: String,
    val gender: String?,
    val age: String?,
    val birthday: String?,
    val death: String?,
    val yearsActiveStart: Int?,
    val yearsActiveEnd: Int?,
    val homeTown: String?,
    val occupations: String?,
    val bloodType: String?,
    val favourites: Int?,
    val isFavourite: Boolean,
    val media: StaffMediaPage,
    val characters: StaffCharacterPage,
)

internal data class StaffMediaItem(
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val palette: List<Color>,
    val staffRole: String?,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
)

internal data class StaffMediaPage(
    val items: List<StaffMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal data class StaffCharacterItem(
    val characterId: Int,
    val name: String,
    val imageUrl: String?,
    val mediaTitle: String?,
)

internal data class StaffCharacterPage(
    val items: List<StaffCharacterItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal enum class StaffMediaSort(val apiValue: String) {
    NEWEST("START_DATE_DESC"),
    OLDEST("START_DATE"),
    POPULARITY("POPULARITY_DESC"),
    FAVOURITES("FAVOURITES_DESC"),
    SCORE("SCORE_DESC"),
    TITLE("TITLE_ROMAJI"),
}
