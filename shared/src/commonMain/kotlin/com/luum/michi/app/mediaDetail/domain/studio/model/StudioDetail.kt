package com.luum.michi.app.mediaDetail.domain.studio.model

import com.luum.michi.app.mediaDetail.domain.model.MediaGridPage

internal data class StudioDetail(
    val id: Int,
    val name: String,
    val isAnimeStudio: Boolean,
    val favourites: Int?,
    val isFavourite: Boolean,
    val media: MediaGridPage,
)

/** Canonical AniList page for this studio. */
internal fun StudioDetail.aniListUrl(): String = "https://anilist.co/studio/$id"

/** Body used by the share sheet: name + canonical URL. */
internal fun StudioDetail.shareText(): String = "$name\n${aniListUrl()}"
