package com.luum.michi.app.characterDetail.presentation.model

import androidx.compose.ui.graphics.Color

internal data class CharacterDetail(
    val id: Int,
    val name: String,
    val nativeName: String?,
    val alternativeNames: List<String>,
    val alternativeSpoilerNames: List<String>,
    val imageUrl: String?,
    val descriptionPlain: String,
    val gender: String?,
    val age: String?,
    val bloodType: String?,
    val birthday: String?,
    val favourites: Int?,
    val isFavourite: Boolean,
    val media: CharacterMediaPage,
)

internal data class CharacterMediaItem(
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val palette: List<Color>,
    val role: String?,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
    val viewerStatus: String?,
    val voiceActorName: String?,
    val voiceActorImageUrl: String?,
    val voiceActorLanguage: String?,
    val voiceActorId: Int?,
)

internal data class CharacterMediaPage(
    val items: List<CharacterMediaItem>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal enum class CharacterMediaSort(val apiValue: String) {
    POPULARITY("POPULARITY_DESC"),
    NEWEST("START_DATE_DESC"),
    OLDEST("START_DATE"),
    FAVOURITES("FAVOURITES_DESC"),
    SCORE("SCORE_DESC"),
}
