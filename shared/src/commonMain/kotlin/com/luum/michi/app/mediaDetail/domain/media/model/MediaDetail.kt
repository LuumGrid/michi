package com.luum.michi.app.mediaDetail.domain.media.model

import com.luum.michi.app.core.anilist.medialist.MediaListViewerEntry

internal enum class MediaDetailType { ANIME, MANGA, UNKNOWN }

internal data class StudioRef(val id: Int, val name: String)

internal data class MediaDetail(
    val id: Int,
    val type: MediaDetailType,
    val title: String,
    val coverUrl: String?,
    val coverFullUrl: String? = null,
    val bannerUrl: String?,
    val paletteHex: String?,
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
    val viewerEntry: MediaListViewerEntry?,
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
    val paletteHex: String?,
    val kind: MediaRelationKind,
    val format: String?,
    val year: Int?,
    val averageScore: Int?,
    val favourites: Int?,
    val viewerStatus: String?,
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
