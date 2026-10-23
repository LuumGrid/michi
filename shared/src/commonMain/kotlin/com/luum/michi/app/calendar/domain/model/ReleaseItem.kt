package com.luum.michi.app.calendar.domain.model

import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.core.media.domain.MediaFormat
import com.luum.michi.app.core.media.domain.MediaSeason
import com.luum.michi.app.core.media.domain.MediaWorkStatus
import com.luum.michi.app.core.model.CoverSizes

internal data class ReleaseItem(
    val title: String,
    /** Aired episode number; the "Ep. N" label is localized at the UI boundary. */
    val episode: Int,
    val airingAtEpoch: Long,
    val id: Int? = null,
    val cover: CoverSizes? = null,
    val format: MediaFormat? = null,
    val mediaStatus: MediaWorkStatus? = null,
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
