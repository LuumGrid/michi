package com.luum.michi.app.calendar.domain.model

import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.model.MediaSeason

internal data class ReleaseItem(
    val title: String,
    val release: String,
    val time: String,
    val paletteHex: String?,
    val id: Int? = null,
    val coverUrl: String? = null,
    val averageScore: Int? = null,
    val favourites: Int? = null,
    val popularity: Int? = null,
    val isUserFavorited: Boolean = false,
    val isUserRanked: Boolean = false,
    val userStatus: MediaListStatus? = null,
    val season: MediaSeason? = null,
    val seasonYear: Int? = null,
    val streamingPlatforms: List<StreamingPlatform> = emptyList(),
)

internal data class StreamingPlatform(
    val site: String,
    val url: String,
    val iconUrl: String? = null,
    val color: String? = null,
)
