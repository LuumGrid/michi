package com.luum.michi.app.core.media.repository

import com.luum.michi.app.core.media.repository.dto.MediaTitleDto

/**
 * Single shared title fallback chain for AniList media titles.
 *
 * Replaces the hand-maintained per-feature copies (`bestTitle`) so the chain
 * cannot drift apart again: a change here applies to lists, detail, calendar,
 * dashboard, favorites and search at once. (Enum-label casing lives in
 * `core/media/domain` next to [com.luum.michi.app.core.media.domain.MediaFormat], usable from any layer.)
 */
internal fun MediaTitleDto?.bestTitle(): String {
    if (this == null) return ""
    return userPreferred ?: english ?: romaji ?: native ?: ""
}

/**
 * Nullable twin of [bestTitle]: a null title stays null so a caller can fall
 * back to something else (the notifications mapper falls back to the
 * deleted-media title, which [bestTitle]'s empty-string default would mask).
 */
internal fun MediaTitleDto?.bestTitleOrNull(): String? {
    if (this == null) return null
    return userPreferred ?: english ?: romaji ?: native
}

/**
 * Every non-blank title variant, deduped, in a stable order (romaji, english,
 * native). The anime/manga lists show the alternate titles under the main one.
 */
internal fun MediaTitleDto?.allTitles(): List<String> {
    if (this == null) return emptyList()
    return listOfNotNull(
        romaji?.takeIf { it.isNotBlank() },
        english?.takeIf { it.isNotBlank() },
        native?.takeIf { it.isNotBlank() },
    ).distinct()
}
