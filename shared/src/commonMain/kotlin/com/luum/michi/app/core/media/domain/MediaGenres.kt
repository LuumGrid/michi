package com.luum.michi.app.core.media.domain

/**
 * Static genre list shared by the anime/manga list filter sheet and the Explore
 * filter sheet. AniList's genres are essentially stable, so the list is compiled
 * in instead of spending a request on `GenreCollection`. "Hentai" is not here:
 * Explore appends it only while the 18+ gate allows adult content.
 */
internal val MediaGenres: List<String> = listOf(
    "Action",
    "Adventure",
    "Comedy",
    "Drama",
    "Ecchi",
    "Fantasy",
    "Horror",
    "Mahou Shoujo",
    "Mecha",
    "Music",
    "Mystery",
    "Psychological",
    "Romance",
    "Sci-Fi",
    "Slice of Life",
    "Sports",
    "Supernatural",
    "Thriller",
)
