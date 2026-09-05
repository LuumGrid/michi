package com.luum.michi.app.discover.presentation.explore.model

import com.luum.michi.app.core.media.currentSeasonAndYear
import com.luum.michi.app.core.platform.components.PlatformFilterOption
import com.luum.michi.app.discover.presentation.explore.state.ExploreCategory

internal data class ExploreFormatOption(val value: String, val label: String)

internal fun ExploreCategory.filterLabel(isSpanish: Boolean): String = when (this) {
    ExploreCategory.ANIME -> "Anime"
    ExploreCategory.MANGA -> "Manga"
    ExploreCategory.CHARACTERS -> if (isSpanish) "Personajes" else "Characters"
    ExploreCategory.STAFF -> "Staff"
    ExploreCategory.STUDIOS -> if (isSpanish) "Estudios" else "Studios"
}

internal fun exploreGenres(): List<String> {
    return listOf(
        "Action", "Adventure", "Comedy", "Drama", "Fantasy",
        "Horror", "Mecha", "Music", "Mystery", "Romance", "Sci-Fi",
        "Slice of Life", "Sports", "Supernatural", "Thriller",
    )
}

internal fun exploreFormats(isSpanish: Boolean): List<ExploreFormatOption> = if (isSpanish) {
    listOf(
        ExploreFormatOption("TV", "TV"),
        ExploreFormatOption("MOVIE", "Películas"),
        ExploreFormatOption("OVA", "OVAs"),
        ExploreFormatOption("ONA", "ONAs"),
        ExploreFormatOption("SPECIAL", "Especiales"),
        ExploreFormatOption("MANGA", "Mangas"),
        ExploreFormatOption("NOVEL", "Novelas"),
        ExploreFormatOption("ONE_SHOT", "One-shots"),
    )
} else {
    listOf(
        ExploreFormatOption("TV", "TV"),
        ExploreFormatOption("MOVIE", "Movies"),
        ExploreFormatOption("OVA", "OVAs"),
        ExploreFormatOption("ONA", "ONAs"),
        ExploreFormatOption("SPECIAL", "Specials"),
        ExploreFormatOption("MANGA", "Manga"),
        ExploreFormatOption("NOVEL", "Novels"),
        ExploreFormatOption("ONE_SHOT", "One-shots"),
    )
}

/**
 * Años seleccionables: del año siguiente al actual (cubre la "próxima temporada"
 * cuando cae en enero del año que viene) hacia atrás, con la cola curada clásica.
 */
internal fun exploreYears(): List<Int?> {
    val currentYear = currentSeasonAndYear().year
    return listOf(null) + ((currentYear + 1) downTo 2011) + listOf(2010, 2005, 2000)
}

internal fun exploreSeasonOptions(isSpanish: Boolean): List<PlatformFilterOption> {
    val any = if (isSpanish) "Cualquier temporada" else "Any season"
    val labels = if (isSpanish) {
        listOf("Invierno", "Primavera", "Verano", "Otoño")
    } else {
        listOf("Winter", "Spring", "Summer", "Fall")
    }
    val ids = listOf("WINTER", "SPRING", "SUMMER", "FALL")
    return listOf(PlatformFilterOption("any", any)) +
        ids.zip(labels) { id, label -> PlatformFilterOption(id, label) }
}
