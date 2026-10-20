package com.luum.michi.app.core.navigation.domain

/**
 * Matches an AniList entity link inside free text (a bare URL or a shared
 * message): anime, manga, character, staff or studio with a numeric id. The
 * leading boundary keeps other hosts (including subdomains such as
 * `evil.anilist.co`) from matching; slugs, queries and fragments are ignored
 * because the pattern stops at the id.
 */
private val aniListDestinationPattern = Regex(
    pattern = "(?:^|[^\\w.-])(?:https?://)?(?:www\\.)?anilist\\.co/" +
        "(anime|manga|character|staff|studio)/(\\d+)(?!\\w)",
    option = RegexOption.IGNORE_CASE,
)

/**
 * Extracts the first valid AniList destination from a web link or shared text
 * so opening `https://anilist.co/{anime|manga|character|staff|studio}/{id}`
 * (or sharing a message that contains it) can open the app's detail page.
 * Returns null for users, activities, forums, other hosts and malformed input.
 * Plain string work (no `java.net.URI`) so it works across KMP targets.
 */
internal fun parseAniListDestination(raw: String?): DetailDestination? {
    if (raw.isNullOrBlank()) return null
    val match = aniListDestinationPattern.find(raw) ?: return null
    val id = match.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
    return when (match.groupValues[1].lowercase()) {
        "anime", "manga" -> DetailDestination.Media(id)
        "character" -> DetailDestination.Character(id)
        "staff" -> DetailDestination.Staff(id)
        "studio" -> DetailDestination.Studio(id)
        else -> null
    }
}
