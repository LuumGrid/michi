package com.luum.michi.app.core.network.repository.dto

/**
 * Single shared title fallback chain for AniList media titles.
 *
 * Replaces the hand-maintained per-feature copies (`bestTitle`) so the chain
 * cannot drift apart again: a change here applies to lists, detail, calendar,
 * dashboard, favorites and search at once. (Enum-label casing lives in
 * `core/model` next to [MediaFormat], usable from any layer.)
 *
 * The notifications mapper keeps its own nullable variant on purpose: there a
 * null title means "fall back to the deleted-media title", which this
 * empty-string default would mask.
 */
internal fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}
