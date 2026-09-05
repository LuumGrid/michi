package com.luum.michi.app.manga.domain.model

/**
 * Typed AniList media format. Parsed once at the data boundary so the rest of
 * the feature never branches on raw strings. Unknown or missing values map to
 * [UNKNOWN] instead of falling through silently.
 */
internal enum class MangaMediaFormat {
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

internal fun parseMangaMediaFormat(raw: String?): MangaMediaFormat = when (raw?.uppercase()) {
    "TV" -> MangaMediaFormat.TV
    "TV_SHORT" -> MangaMediaFormat.TV_SHORT
    "MOVIE" -> MangaMediaFormat.MOVIE
    "SPECIAL" -> MangaMediaFormat.SPECIAL
    "OVA" -> MangaMediaFormat.OVA
    "ONA" -> MangaMediaFormat.ONA
    "MUSIC" -> MangaMediaFormat.MUSIC
    "MANGA" -> MangaMediaFormat.MANGA
    "NOVEL" -> MangaMediaFormat.NOVEL
    "ONE_SHOT" -> MangaMediaFormat.ONE_SHOT
    else -> MangaMediaFormat.UNKNOWN
}

/** Display label for the format ("Manga", "Novel", "One shot", ...). */
internal fun MangaMediaFormat.label(): String = when (this) {
    MangaMediaFormat.UNKNOWN -> "Manga"
    else -> name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

/** Whether entries of this format track progress by volumes instead of chapters. */
internal fun MangaMediaFormat.isVolumeBased(): Boolean = this == MangaMediaFormat.NOVEL
