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
    val priority: Int,
    val isPrivate: Boolean,
    val hiddenFromStatusLists: Boolean,
    val startedAtMillis: Long?,
    val completedAtMillis: Long?,
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
    val isFavourite: Boolean,
    val viewerEntry: MediaDetailViewerEntry?,
    val relations: List<MediaDetailRelation>,
)

internal enum class MediaRelationKind {
    SEQUEL, PREQUEL, SIDE_STORY, SPIN_OFF, PARENT, ADAPTATION, OTHER
}

internal data class MediaDetailRelation(
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val palette: List<Color>,
    val kind: MediaRelationKind,
    val format: String?,
)
