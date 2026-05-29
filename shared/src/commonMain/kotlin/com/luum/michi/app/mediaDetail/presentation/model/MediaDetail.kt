package com.luum.michi.app.mediaDetail.presentation.model

import androidx.compose.ui.graphics.Color

internal enum class MediaDetailType { ANIME, MANGA, UNKNOWN }

internal data class StudioRef(val id: Int, val name: String)

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
    val studios: List<StudioRef>,
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
    val scoreDistribution: List<MediaScoreBucket>,
    val statusDistribution: List<MediaStatusBucket>,
    val characters: MediaCharactersPage,
    val staff: MediaStaffPage,
)

internal data class MediaScoreBucket(val score: Int, val amount: Int)

internal enum class MediaStatsStatus {
    CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING, OTHER,
}

internal data class MediaStatusBucket(val status: MediaStatsStatus, val amount: Int)

internal enum class MediaCharacterRole { MAIN, SUPPORTING, BACKGROUND, OTHER }

internal data class MediaVoiceActor(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val language: String?,
)

internal data class MediaCharacterEntry(
    val edgeKey: String,
    val characterId: Int,
    val name: String,
    val imageUrl: String?,
    val role: MediaCharacterRole,
    val voiceActor: MediaVoiceActor?,
)

internal data class MediaCharactersPage(
    val items: List<MediaCharacterEntry>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal data class MediaStaffEntry(
    val edgeKey: String,
    val staffId: Int,
    val name: String,
    val imageUrl: String?,
    val role: String?,
)

internal data class MediaStaffPage(
    val items: List<MediaStaffEntry>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal enum class MediaRelationKind {
    SEQUEL, PREQUEL, SIDE_STORY, SPIN_OFF, PARENT, ADAPTATION, ALTERNATIVE, SOURCE, SUMMARY, CHARACTER, OTHER
}

internal data class MediaDetailRelation(
    val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val palette: List<Color>,
    val kind: MediaRelationKind,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
    val favourites: Int?,
    val viewerStatus: String?,
)

internal data class MediaReviewEntry(
    val id: Int,
    val summary: String,
    val rating: String,
    val reviewerName: String,
    val reviewerImageUrl: String?,
    val text: String,
)

internal data class MediaReviewsPage(
    val items: List<MediaReviewEntry>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal data class MediaThreadEntry(
    val id: Int,
    val title: String,
    val authorName: String,
    val authorImageUrl: String?,
    val replyCount: Int,
    val viewCount: Int,
    val createdAt: Long,
)

internal data class MediaThreadsPage(
    val items: List<MediaThreadEntry>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal data class MediaFollowingEntry(
    val id: Int,
    val userName: String,
    val userImageUrl: String?,
    val progressLabel: String,
    val scoreLabel: String,
)

internal data class MediaActivityEntry(
    val id: Int,
    val userName: String,
    val userImageUrl: String?,
    val actionText: String,
    val timeLabel: String,
    val likesCount: Int,
)

internal data class MediaActivitiesPage(
    val items: List<MediaActivityEntry>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)

internal data class MediaRecommendationEntry(
    val id: Int,
    val title: String,
    val coverUrl: String?,
    val format: String?,
    val year: Int?,
    val episodesCount: Int?,
    val chaptersCount: Int?,
    val volumesCount: Int?,
    val averageScore: Int?,
    val favouritesCount: Int?,
    val likesCount: Int,
    val viewerStatus: String?,
)
