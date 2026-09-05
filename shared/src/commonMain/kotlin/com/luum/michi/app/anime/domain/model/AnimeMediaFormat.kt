package com.luum.michi.app.anime.domain.model

/**
 * Typed AniList media format. Parsed once at the data boundary so the rest of
 * the feature never branches on raw strings. Unknown or missing values map to
 * [UNKNOWN] instead of falling through silently.
 */
internal enum class AnimeMediaFormat {
    TV,
    TV_SHORT,
    MOVIE,
    SPECIAL,
    OVA,
    ONA,
    MUSIC,
    MANGA,
    NOVEL,
    ONE_SHOT,
    UNKNOWN,
}

internal fun parseAnimeMediaFormat(raw: String?): AnimeMediaFormat = when (raw?.uppercase()) {
    "TV" -> AnimeMediaFormat.TV
    "TV_SHORT" -> AnimeMediaFormat.TV_SHORT
    "MOVIE" -> AnimeMediaFormat.MOVIE
    "SPECIAL" -> AnimeMediaFormat.SPECIAL
    "OVA" -> AnimeMediaFormat.OVA
    "ONA" -> AnimeMediaFormat.ONA
    "MUSIC" -> AnimeMediaFormat.MUSIC
    "MANGA" -> AnimeMediaFormat.MANGA
    "NOVEL" -> AnimeMediaFormat.NOVEL
    "ONE_SHOT" -> AnimeMediaFormat.ONE_SHOT
    else -> AnimeMediaFormat.UNKNOWN
}

/** Display label for the format ("Tv", "Tv short", "Movie", ...). */
internal fun AnimeMediaFormat.label(): String = when (this) {
    AnimeMediaFormat.UNKNOWN -> "Anime"
    else -> name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

/** Completed list section matching this format. Exhaustive over the enum. */
internal fun AnimeMediaFormat.completedSection(): AnimeListSection = when (this) {
    AnimeMediaFormat.MOVIE -> AnimeListSection.COMPLETED_MOVIE
    AnimeMediaFormat.OVA -> AnimeListSection.COMPLETED_OVA
    AnimeMediaFormat.ONA -> AnimeListSection.COMPLETED_ONA
    AnimeMediaFormat.TV_SHORT -> AnimeListSection.COMPLETED_TV_SHORT
    AnimeMediaFormat.SPECIAL -> AnimeListSection.COMPLETED_SPECIAL
    AnimeMediaFormat.TV,
    AnimeMediaFormat.MUSIC,
    AnimeMediaFormat.MANGA,
    AnimeMediaFormat.NOVEL,
    AnimeMediaFormat.ONE_SHOT,
    AnimeMediaFormat.UNKNOWN -> AnimeListSection.COMPLETED_TV
}
