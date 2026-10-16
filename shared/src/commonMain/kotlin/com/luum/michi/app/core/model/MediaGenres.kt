package com.luum.michi.app.core.model

/**
 * Static genre list for the anime/manga list filters. Explore's filter sheet
 * will reuse it when its UI lands (round 3); today `mediaList` is the only
 * consumer.
 */
internal val MediaGenres: List<String> = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy",
    "Horror", "Mecha", "Music", "Mystery", "Romance", "Sci-Fi",
    "Slice of Life", "Sports", "Supernatural", "Thriller",
)
