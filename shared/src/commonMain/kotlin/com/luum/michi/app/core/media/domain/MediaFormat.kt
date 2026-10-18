package com.luum.michi.app.core.media.domain


internal enum class MediaFormat {
    TV, TV_SHORT, MOVIE, SPECIAL, OVA, ONA, MUSIC, MANGA, NOVEL, ONE_SHOT, UNKNOWN,
}


internal fun parseMediaFormat(raw: String?): MediaFormat = when (raw?.uppercase()) {
    "TV" -> MediaFormat.TV
    "TV_SHORT" -> MediaFormat.TV_SHORT
    "MOVIE" -> MediaFormat.MOVIE
    "SPECIAL" -> MediaFormat.SPECIAL
    "OVA" -> MediaFormat.OVA
    "ONA" -> MediaFormat.ONA
    "MUSIC" -> MediaFormat.MUSIC
    "MANGA" -> MediaFormat.MANGA
    "NOVEL" -> MediaFormat.NOVEL
    "ONE_SHOT" -> MediaFormat.ONE_SHOT
    else -> MediaFormat.UNKNOWN
}

/**
 * The formats a media type can actually have, in AniList vocabulary: anime
 * excludes the reading formats; manga keeps only MANGA/NOVEL/ONE_SHOT. The one
 * source both the Explore filter and the user lists' filter reuse, so the two
 * surfaces can never disagree on what a type offers.
 */
internal fun mediaFormatsForType(isAnime: Boolean): List<MediaFormat> =
    MediaFormat.entries.filter {
        if (isAnime) {
            it != MediaFormat.MANGA && it != MediaFormat.NOVEL &&
                it != MediaFormat.ONE_SHOT && it != MediaFormat.UNKNOWN
        } else {
            it == MediaFormat.MANGA || it == MediaFormat.NOVEL || it == MediaFormat.ONE_SHOT
        }
    }


/** Label derivado del nombre del enum ("TV Short", "One Shot"...) — vocabulario
 *  de AniList, igual en todos los idiomas, no es copy traducible. Los acrónimos
 *  (TV, OVA, ONA) se mantienen en mayúsculas como los muestra AniList.
 *  [unknownFallback] deja que cada caller decida qué mostrar si el formato
 *  viene nulo/desconocido (ej. "Anime" vs "Manga", o el label localizado). */
internal fun MediaFormat.label(unknownFallback: String): String = when (this) {
    MediaFormat.UNKNOWN -> unknownFallback
    else -> name.toTitleCase()
}

/** `"TV_SHORT"` -> `"TV Short"`. Single shared casing for API enum labels
 *  (format/season/source/status); lives next to [MediaFormat] so both model
 *  and repository layers use it without layer violations. */
internal fun String.toTitleCase(): String = this
    .replace('_', ' ')
    .lowercase()
    .split(' ')
    .joinToString(" ") { word ->
        when (word.uppercase()) {
            "TV", "OVA", "ONA" -> word.uppercase()
            else -> word.replaceFirstChar { it.uppercase() }
        }
    }
