package com.luum.michi.app.mediaDetail.presentation.model

import androidx.compose.ui.graphics.Color

internal enum class MediaDetailType { ANIME, MANGA, UNKNOWN }

internal data class MediaDetailViewerEntry(
    val id: Int,
    val status: MediaListStatus?,
    val progress: Int,
    val progressVolumes: Int?,
    val score: Float,
    val notes: String,
    val repeat: Int,
    val isPrivate: Boolean,
    val hiddenFromStatusLists: Boolean,
)

internal data class MediaDetail(
    val id: Int,
    val type: MediaDetailType,
    val title: String,
    val coverUrl: String?,
    val bannerUrl: String?,
    val palette: List<Color>,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val chapters: Int?,
    val volumes: Int?,
    val duration: Int?,
    val genres: List<String>,
    val studios: List<String>,
    val source: String?,
    val season: String?,
    val startedLabel: String?,
    val endedLabel: String?,
    val averageScore: Int?,
    val meanScore: Int?,
    val popularity: Int?,
    val favourites: Int?,
    val descriptionPlain: String,
    val isAdult: Boolean,
    val viewerEntry: MediaDetailViewerEntry?,
)
