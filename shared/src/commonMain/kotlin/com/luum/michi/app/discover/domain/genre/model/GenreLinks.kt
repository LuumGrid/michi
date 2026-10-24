package com.luum.michi.app.discover.domain.genre.model

import com.luum.michi.app.core.media.domain.MediaType

/**
 * Canonical AniList search page for a genre in one catalog. AniList has no
 * genre entity, so the closest stable page is the catalog search filtered by
 * genre; the name is percent-encoded as a query value.
 */
internal fun genreAniListUrl(name: String, type: MediaType): String {
    val segment = if (type == MediaType.MANGA) "manga" else "anime"
    return "https://anilist.co/search/$segment?genres=${name.urlEncodeQueryValue()}"
}

/** Body used by the share sheet: genre name + canonical URL. */
internal fun genreShareText(name: String, type: MediaType): String = "$name\n${genreAniListUrl(name, type)}"
