package com.luum.michi.app.core.navigation.domain

/**
 * Matches an AniList anime/manga link inside free text (a bare URL or a shared
 * message). Only `anime`/`manga` paths with a numeric id match; user/profile
 * links and other hosts do not.
 */
private val aniListMediaPattern = Regex(
    pattern = "(?:https?://)?(?:www\\.)?anilist\\.co/(anime|manga)/(\\d+)",
    option = RegexOption.IGNORE_CASE,
)

/**
 * Extracts a media id from an AniList web link so tapping
 * `https://anilist.co/anime/{id}` (or `/manga/{id}`) — or sharing a message
 * that contains it — can open the app's detail page. Returns null for user
 * links, other hosts and malformed input. Plain string work (no `java.net.URI`)
 * so it works across KMP targets.
 */
internal fun parseAniListMediaId(raw: String?): Int? {
    if (raw.isNullOrBlank()) return null
    val match = aniListMediaPattern.find(raw) ?: return null
    return match.groupValues[2].toIntOrNull()?.takeIf { it > 0 }
}
