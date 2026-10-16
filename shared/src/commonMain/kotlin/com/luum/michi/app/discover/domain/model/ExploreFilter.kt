package com.luum.michi.app.discover.domain.model

import com.luum.michi.app.core.model.MediaFormat
import com.luum.michi.app.core.model.label
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.currentSeasonAndYear
import com.luum.michi.app.core.model.FilterOption

internal data class ExploreFormatOption(val value: String, val label: String)

internal fun ExploreCategory.filterLabel(strings: LanguageStrings): String = when (this) {
    ExploreCategory.ANIME -> strings.animeLabel
    ExploreCategory.MANGA -> strings.mangaLabel
    ExploreCategory.CHARACTERS -> strings.charactersLabel
    ExploreCategory.STAFF -> strings.mediaDetailTabStaff
    ExploreCategory.STUDIOS -> strings.studiosLabel
}

internal fun exploreFormats(strings: LanguageStrings): List<ExploreFormatOption> =
    MediaFormat.entries
        .filter { it != MediaFormat.UNKNOWN }
        .map { ExploreFormatOption(it.name, it.label(strings.formatUnknownLabel)) }

/**
 * Años seleccionables: del año siguiente al actual (cubre la "próxima temporada"
 * cuando cae en enero del año que viene) hacia atrás, con la cola curada clásica.
 */
internal fun exploreYears(): List<Int?> {
    val currentYear = currentSeasonAndYear().year
    return listOf(null) + ((currentYear + 1) downTo 2011) + listOf(2010, 2005, 2000)
}

internal fun exploreSeasonOptions(strings: LanguageStrings): List<FilterOption> {
    val labels = listOf(
        strings.seasonWinterLabel,
        strings.seasonSpringLabel,
        strings.seasonSummerLabel,
        strings.seasonFallLabel,
    )
    val ids = listOf("WINTER", "SPRING", "SUMMER", "FALL")
    return listOf(FilterOption("any", strings.exploreAnySeasonLabel)) +
        ids.zip(labels) { id, label -> FilterOption(id, label) }
}
