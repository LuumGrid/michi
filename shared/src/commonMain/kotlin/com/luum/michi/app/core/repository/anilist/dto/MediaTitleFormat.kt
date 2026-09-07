package com.luum.michi.app.core.repository.anilist.dto

/**
 * Single shared formatting of AniList API-provided strings for display.
 *
 * Replaces the hand-maintained per-feature copies (`bestTitle`, `toTitleCase`)
 * so the title fallback chain and the enum-label casing cannot drift apart
 * again: a change here applies to lists, detail, calendar, dashboard, favorites
 * and search at once.
 *
 * The notifications mapper keeps its own nullable variant on purpose: there a
 * null title means "fall back to the deleted-media title", which this
 * empty-string default would mask.
 */
internal fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

/** `"TV_SHORT"` -> `"Tv Short"`. Used for format/season/source/status labels. */
internal fun String.toTitleCase(): String = this
    .replace('_', ' ')
    .lowercase()
    .split(' ')
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
