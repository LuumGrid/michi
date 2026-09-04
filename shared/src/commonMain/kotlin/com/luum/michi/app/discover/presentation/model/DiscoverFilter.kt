package com.luum.michi.app.discover.presentation.model

import com.luum.michi.app.core.platform.components.PlatformFilterGroup
import com.luum.michi.app.core.platform.components.PlatformFilterOption
import com.luum.michi.app.discover.presentation.state.DiscoverCategory
import com.luum.michi.app.discover.presentation.state.DiscoverStateHolder

internal data class DiscoverFormatOption(val value: String, val label: String)

internal fun DiscoverCategory.filterLabel(isSpanish: Boolean): String = when (this) {
    DiscoverCategory.ANIME -> "Anime"
    DiscoverCategory.MANGA -> "Manga"
    DiscoverCategory.CHARACTERS -> if (isSpanish) "Personajes" else "Characters"
    DiscoverCategory.STAFF -> "Staff"
    DiscoverCategory.STUDIOS -> if (isSpanish) "Estudios" else "Studios"
}

internal fun discoverGenres(isSpanish: Boolean): List<String> {
    val all = if (isSpanish) "Todos" else "All"
    return listOf(
        all, "Action", "Adventure", "Comedy", "Drama", "Fantasy",
        "Horror", "Mecha", "Music", "Mystery", "Romance", "Sci-Fi",
        "Slice of Life", "Sports", "Supernatural", "Thriller",
    )
}

internal fun discoverFormats(isSpanish: Boolean): List<DiscoverFormatOption> = if (isSpanish) {
    listOf(
        DiscoverFormatOption("All", "Todos los formatos"),
        DiscoverFormatOption("TV", "TV"),
        DiscoverFormatOption("MOVIE", "Películas"),
        DiscoverFormatOption("OVA", "OVAs"),
        DiscoverFormatOption("ONA", "ONAs"),
        DiscoverFormatOption("SPECIAL", "Especiales"),
        DiscoverFormatOption("MANGA", "Mangas"),
        DiscoverFormatOption("NOVEL", "Novelas"),
        DiscoverFormatOption("ONE_SHOT", "One-shots"),
    )
} else {
    listOf(
        DiscoverFormatOption("All", "All formats"),
        DiscoverFormatOption("TV", "TV"),
        DiscoverFormatOption("MOVIE", "Movies"),
        DiscoverFormatOption("OVA", "OVAs"),
        DiscoverFormatOption("ONA", "ONAs"),
        DiscoverFormatOption("SPECIAL", "Specials"),
        DiscoverFormatOption("MANGA", "Manga"),
        DiscoverFormatOption("NOVEL", "Novels"),
        DiscoverFormatOption("ONE_SHOT", "One-shots"),
    )
}

internal fun discoverYears(): List<Int?> =
    listOf(null, 2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018, 2017, 2016, 2015, 2014, 2013, 2012, 2011, 2010, 2005, 2000)

internal fun discoverSeasonOptions(isSpanish: Boolean): List<PlatformFilterOption> {
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

/**
 * Grupos del filter sheet (lo que mostraban los chips inline, ya eliminados).
 * En categorías de entidades solo aplica Tipo.
 */
internal fun buildDiscoverFilterGroups(
    stateHolder: DiscoverStateHolder,
    isSpanish: Boolean,
): List<PlatformFilterGroup> {
    val typeGroup = PlatformFilterGroup(
        id = "type",
        title = if (isSpanish) "Tipo" else "Type",
        options = DiscoverCategory.entries.map { category ->
            PlatformFilterOption(id = category.name, label = category.filterLabel(isSpanish))
        },
        selectedId = stateHolder.category.name,
    )
    if (stateHolder.isEntitySearch()) return listOf(typeGroup)

    val groups = mutableListOf(typeGroup)
    // Season solo aplica a anime (la query de manga no tiene season).
    if (stateHolder.category == DiscoverCategory.ANIME) {
        groups += PlatformFilterGroup(
            id = "season",
            title = if (isSpanish) "Temporada" else "Season",
            options = discoverSeasonOptions(isSpanish),
            selectedId = stateHolder.season ?: "any",
        )
    }

    val allLabel = if (isSpanish) "Todos" else "All"
    val genreGroup = PlatformFilterGroup(
        id = "genre",
        title = if (isSpanish) "Género" else "Genre",
        options = discoverGenres(isSpanish).map { genre ->
            val id = if (genre == allLabel) "All" else genre
            PlatformFilterOption(id = id, label = genre)
        },
        selectedId = if (stateHolder.genre == "All" || stateHolder.genre == "Todos") "All" else stateHolder.genre,
    )
    val formats = discoverFormats(isSpanish)
    val formatGroup = PlatformFilterGroup(
        id = "format",
        title = if (isSpanish) "Formato" else "Format",
        options = formats.map { PlatformFilterOption(id = it.value, label = it.label) },
        selectedId = formats
            .firstOrNull { it.value.equals(stateHolder.format, ignoreCase = true) }
            ?.value ?: "All",
    )
    val yearGroup = PlatformFilterGroup(
        id = "year",
        title = if (isSpanish) "Año" else "Year",
        options = discoverYears().map { year ->
            PlatformFilterOption(
                id = year?.toString() ?: "any",
                label = year?.toString() ?: if (isSpanish) "Cualquier año" else "Any year",
            )
        },
        selectedId = stateHolder.year?.toString() ?: "any",
    )
    return groups + listOf(genreGroup, formatGroup, yearGroup)
}

/** Aplica una selección del filter sheet al holder (con su debounce de 300ms). */
internal fun DiscoverStateHolder.applyFilterSelection(groupId: String, optionId: String) {
    when (groupId) {
        "type" -> DiscoverCategory.entries
            .firstOrNull { it.name == optionId }
            ?.let {
                // Al cambiar de tipo se limpia season si deja de aplicar.
                updateFilters(
                    newCategory = it,
                    newSeason = if (it == DiscoverCategory.ANIME) season else null,
                )
            }
        "season" -> updateFilters(newSeason = if (optionId == "any") null else optionId)
        "genre" -> updateFilters(newGenre = optionId)
        "format" -> updateFilters(newFormat = optionId)
        "year" -> updateFilters(newYear = if (optionId == "any") null else optionId.toIntOrNull())
    }
}
