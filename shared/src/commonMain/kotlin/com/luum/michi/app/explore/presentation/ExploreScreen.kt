package com.luum.michi.app.explore.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.components.DiscoverFilterSheet
import com.luum.michi.app.core.platform.components.DiscoverSortField
import com.luum.michi.app.core.platform.components.combineDiscoverSort
import com.luum.michi.app.core.platform.components.parseDiscoverSort
import com.luum.michi.app.core.platform.components.PlatformFilterChip
import com.luum.michi.app.explore.presentation.state.ExploreCategory
import com.luum.michi.app.explore.presentation.state.ExploreStateHolder
import com.luum.michi.app.search.presentation.components.SearchResultCard

@Composable
internal fun ExploreScreen(
    stateHolder: ExploreStateHolder,
    showSortSheet: Boolean,
    onDismissSortSheet: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings
    val isSpanish = strings.languageLabel.equals("Idioma", ignoreCase = true)

    LaunchedEffect(stateHolder) {
        if (stateHolder.results.isEmpty() && !stateHolder.isLoading) {
            stateHolder.load()
        }
    }

    // Type (category)
    val categories = listOf(
        ExploreCategory.ANIMATION,
        ExploreCategory.READING,
        ExploreCategory.CHARACTERS,
        ExploreCategory.STAFF,
        ExploreCategory.STUDIOS,
    )
    fun ExploreCategory.label(): String = when (this) {
        ExploreCategory.ANIMATION -> if (isSpanish) "Animación" else "Animation"
        ExploreCategory.READING -> if (isSpanish) "Lectura" else "Reading"
        ExploreCategory.CHARACTERS -> if (isSpanish) "Personajes" else "Characters"
        ExploreCategory.STAFF -> "Staff"
        ExploreCategory.STUDIOS -> if (isSpanish) "Estudios" else "Studios"
    }

    // Genres
    val allLabel = if (isSpanish) "Todos" else "All"
    val genres = listOf(
        allLabel, "Action", "Adventure", "Comedy", "Drama", "Fantasy",
        "Horror", "Mecha", "Music", "Mystery", "Romance", "Sci-Fi",
        "Slice of Life", "Sports", "Supernatural", "Thriller",
    )
    fun isAll(value: String) = value == "All" || value == "Todos"

    // Formats
    val formatOptions = if (isSpanish) {
        listOf(
            FormatOption("All", "Todos los formatos"),
            FormatOption("TV", "TV"),
            FormatOption("MOVIE", "Películas"),
            FormatOption("OVA", "OVAs"),
            FormatOption("ONA", "ONAs"),
            FormatOption("SPECIAL", "Especiales"),
            FormatOption("MANGA", "Mangas"),
            FormatOption("NOVEL", "Novelas"),
            FormatOption("ONE_SHOT", "One-shots"),
        )
    } else {
        listOf(
            FormatOption("All", "All formats"),
            FormatOption("TV", "TV"),
            FormatOption("MOVIE", "Movies"),
            FormatOption("OVA", "OVAs"),
            FormatOption("ONA", "ONAs"),
            FormatOption("SPECIAL", "Specials"),
            FormatOption("MANGA", "Manga"),
            FormatOption("NOVEL", "Novels"),
            FormatOption("ONE_SHOT", "One-shots"),
        )
    }
    val currentFormatOption = formatOptions.find { it.value.equals(stateHolder.format, ignoreCase = true) } ?: formatOptions.first()

    // Years
    val anyYearLabel = if (isSpanish) "Cualquier año" else "Any year"
    val yearOptions: List<Int?> = listOf(null, 2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018, 2017, 2016, 2015, 2014, 2013, 2012, 2011, 2010, 2005, 2000)

    // Sort fields for the sort & order sheet
    val sortFields = listOf(
        DiscoverSortField("POPULARITY", if (isSpanish) "Popularidad" else "Popularity"),
        DiscoverSortField("SCORE", if (isSpanish) "Puntaje" else "Score"),
        DiscoverSortField("TRENDING", if (isSpanish) "Tendencia" else "Trending"),
        DiscoverSortField("START_DATE", if (isSpanish) "Fecha de inicio" else "Start date"),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlatformFilterChip(
                label = if (isSpanish) "Tipo" else "Type",
                selectedOption = stateHolder.category,
                options = categories,
                optionLabel = { it.label() },
                onSelect = { stateHolder.updateFilters(newCategory = it) },
            )
            if (!stateHolder.isEntitySearch()) {
                PlatformFilterChip(
                    label = if (isSpanish) "Género" else "Genre",
                    selectedOption = stateHolder.genre,
                    options = genres,
                    optionLabel = { if (isAll(it)) allLabel else it },
                    onSelect = { stateHolder.updateFilters(newGenre = it) },
                )
                PlatformFilterChip(
                    label = if (isSpanish) "Formato" else "Format",
                    selectedOption = currentFormatOption,
                    options = formatOptions,
                    optionLabel = { it.label },
                    onSelect = { stateHolder.updateFilters(newFormat = it.value) },
                )
                PlatformFilterChip(
                    label = if (isSpanish) "Año" else "Year",
                    selectedOption = stateHolder.year,
                    options = yearOptions,
                    optionLabel = { it?.toString() ?: anyYearLabel },
                    onSelect = { stateHolder.updateFilters(newYear = it) },
                )
                val (currentSortField, _) = parseDiscoverSort(stateHolder.sort)
                val selectedSortField = sortFields.firstOrNull { it.field == currentSortField } ?: sortFields.first()
                PlatformFilterChip(
                    label = if (isSpanish) "Orden" else "Sort",
                    selectedOption = selectedSortField,
                    options = sortFields,
                    optionLabel = { it.label },
                    onSelect = { field ->
                        val (_, desc) = parseDiscoverSort(stateHolder.sort)
                        stateHolder.updateFilters(newSort = combineDiscoverSort(field.field, desc))
                    },
                )
            }
        }

        // Grid of results
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when {
                stateHolder.isLoading && stateHolder.results.isEmpty() ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                stateHolder.error != null && stateHolder.results.isEmpty() ->
                    CenteredMessage(
                        text = stateHolder.error?.let { strings.networkErrorMessage(it) } ?: "",
                        isError = true,
                    )
                stateHolder.results.isEmpty() ->
                    CenteredMessage(
                        text = if (isSpanish) {
                            "No se encontraron resultados en el catálogo."
                        } else {
                            "No catalog results found."
                        }
                    )
                else -> {
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(gridState, stateHolder.hasNextPage) {
                        snapshotFlow {
                            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        }.collect { last ->
                            if (last >= stateHolder.results.size - 4 &&
                                stateHolder.hasNextPage &&
                                !stateHolder.isLoadingMore
                            ) {
                                stateHolder.loadMore()
                            }
                        }
                    }
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(stateHolder.results, key = { "${it.id}_${stateHolder.category.name}" }) { result ->
                            SearchResultCard(
                                result = result,
                                onClick = { onOpenMedia(result.id) },
                                onLongClick = { onEditMedia(result.id) },
                            )
                        }
                        if (stateHolder.isLoadingMore) {
                            item { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                            item { Box(modifier = Modifier.fillMaxWidth()) {} }
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        val (currentSortFieldForSheet, currentDescending) = parseDiscoverSort(stateHolder.sort)
        DiscoverFilterSheet(
            title = if (isSpanish) "Filtros" else "Filters",
            orderTitle = if (isSpanish) "Dirección" else "Order",
            ascendingLabel = if (isSpanish) "Ascendente" else "Ascending",
            descendingLabel = if (isSpanish) "Descendente" else "Descending",
            currentDescending = currentDescending,
            showOnListFilters = !stateHolder.isEntitySearch(),
            hideOnListLabel = if (isSpanish) "Ocultar las de mi lista" else "Hide series on my list",
            onlyOnListLabel = if (isSpanish) "Solo las de mi lista" else "Only show series on my list",
            currentOnList = stateHolder.onList,
            applyLabel = if (isSpanish) "Aplicar" else "Apply",
            onDismiss = onDismissSortSheet,
            onApply = { descending, onList ->
                stateHolder.updateFilters(
                    newSort = combineDiscoverSort(currentSortFieldForSheet, descending),
                    newOnList = onList,
                )
                onDismissSortSheet()
            },
        )
    }
}

@Composable
private fun CenteredMessage(text: String, isError: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private data class FormatOption(val value: String, val label: String)
