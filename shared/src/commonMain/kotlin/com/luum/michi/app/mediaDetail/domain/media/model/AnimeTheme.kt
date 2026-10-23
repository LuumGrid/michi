package com.luum.michi.app.mediaDetail.domain.media.model

internal enum class AnimeThemeKind { OPENING, ENDING }

/**
 * One AnimeThemes song of an anime. [label] is AnimeThemes' slug as given
 * ("OP1", "ED1-TV"); [episodes] joins every entry's range ("1-14, 16, 17-27"),
 * null when none has one; [animeName] is AnimeThemes' anime name, used for the
 * video search; [pageUrl] is the song's page at AnimeThemes.
 */
internal data class AnimeTheme(
    val kind: AnimeThemeKind,
    val label: String,
    val title: String,
    val artists: List<String>,
    val episodes: String?,
    val animeName: String,
    val pageUrl: String,
)
