package com.luum.michi.app.mediaList.domain.anime.model

import com.luum.michi.app.core.domain.model.MediaFormat

/** Completed list section matching this format. Exhaustive over the enum. */
internal fun MediaFormat.completedSection(): AnimeListSection = when (this) {
    MediaFormat.MOVIE -> AnimeListSection.COMPLETED_MOVIE
    MediaFormat.OVA -> AnimeListSection.COMPLETED_OVA
    MediaFormat.ONA -> AnimeListSection.COMPLETED_ONA
    MediaFormat.TV_SHORT -> AnimeListSection.COMPLETED_TV_SHORT
    MediaFormat.SPECIAL -> AnimeListSection.COMPLETED_SPECIAL
    MediaFormat.TV,
    MediaFormat.MUSIC,
    MediaFormat.MANGA,
    MediaFormat.NOVEL,
    MediaFormat.ONE_SHOT,
    MediaFormat.UNKNOWN -> AnimeListSection.COMPLETED_TV
}
