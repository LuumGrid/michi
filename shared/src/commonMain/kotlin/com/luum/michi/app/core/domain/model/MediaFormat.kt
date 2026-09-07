package com.luum.michi.app.core.domain.model


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


/** Label derivado del nombre del enum ("Tv short", "One shot"...) — vocabulario de AniList,
 *  igual en todos los idiomas, no es copy traducible. [unknownFallback] deja que cada caller
 *  decida qué mostrar si el formato viene nulo/desconocido (ej. "Anime" vs "Manga"). */
internal fun MediaFormat.label(unknownFallback: String = "Unknown"): String = when (this) {
    MediaFormat.UNKNOWN -> unknownFallback
    else -> name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}
