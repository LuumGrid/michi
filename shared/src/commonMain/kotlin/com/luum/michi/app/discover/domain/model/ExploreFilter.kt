package com.luum.michi.app.discover.domain.model

import com.luum.michi.app.core.anilist.MediaFormat
import com.luum.michi.app.core.anilist.label
import com.luum.michi.app.core.language.LanguageStrings
import com.luum.michi.app.core.media.currentSeasonAndYear
import com.luum.michi.app.core.platform.components.PlatformFilterOption

internal data class ExploreFormatOption(val value: String, val label: String)

internal fun ExploreCategory.filterLabel(strings: LanguageStrings): String = when (this) {
    ExploreCategory.ANIME -> "Anime"
    ExploreCategory.MANGA -> "Manga"
    ExploreCategory.CHARACTERS -> strings.exploreCharactersCategoryLabel
    ExploreCategory.STAFF -> "Staff"
    ExploreCategory.STUDIOS -> strings.exploreStudiosCategoryLabel
}

internal fun exploreGenres(): List<String> {
    return listOf(
        "Action", "Adventure", "Comedy", "Drama", "Fantasy",
        "Horror", "Mecha", "Music", "Mystery", "Romance", "Sci-Fi",
        "Slice of Life", "Sports", "Supernatural", "Thriller",
    )
}

internal fun exploreFormats(): List<ExploreFormatOption> =
    MediaFormat.entries
        .filter { it != MediaFormat.UNKNOWN }
        .map { ExploreFormatOption(it.name, it.label()) }

/**
 * Años seleccionables: del año siguiente al actual (cubre la "próxima temporada"
 * cuando cae en enero del año que viene) hacia atrás, con la cola curada clásica.
 */
internal fun exploreYears(): List<Int?> {
    val currentYear = currentSeasonAndYear().year
    return listOf(null) + ((currentYear + 1) downTo 2011) + listOf(2010, 2005, 2000)
}

internal fun exploreSeasonOptions(strings: LanguageStrings): List<PlatformFilterOption> {
    val labels = listOf(
        strings.exploreSeasonWinterLabel,
        strings.exploreSeasonSpringLabel,
        strings.exploreSeasonSummerLabel,
        strings.exploreSeasonFallLabel,
    )
    val ids = listOf("WINTER", "SPRING", "SUMMER", "FALL")
    return listOf(PlatformFilterOption("any", strings.exploreAnySeasonLabel)) +
        ids.zip(labels) { id, label -> PlatformFilterOption(id, label) }
}
