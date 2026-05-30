package com.luum.michi.app.seasonal.presentation

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
import com.luum.michi.app.core.media.MediaSeason
import com.luum.michi.app.core.media.currentSeasonAndYear
import com.luum.michi.app.core.platform.components.DiscoverFilterSheet
import com.luum.michi.app.core.platform.components.DiscoverSortField
import com.luum.michi.app.core.platform.components.combineDiscoverSort
import com.luum.michi.app.core.platform.components.parseDiscoverSort
import com.luum.michi.app.core.platform.components.PlatformFilterChip
import com.luum.michi.app.search.presentation.components.SearchResultCard
import com.luum.michi.app.seasonal.presentation.state.SeasonalStateHolder

@Composable
internal fun SeasonalScreen(
    stateHolder: SeasonalStateHolder,
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

    val seasons = listOf(MediaSeason.WINTER, MediaSeason.SPRING, MediaSeason.SUMMER, MediaSeason.FALL)
    fun MediaSeason.label(): String = when (this) {
        MediaSeason.WINTER -> if (isSpanish) "Invierno" else "Winter"
        MediaSeason.SPRING -> if (isSpanish) "Primavera" else "Spring"
        MediaSeason.SUMMER -> if (isSpanish) "Verano" else "Summer"
        MediaSeason.FALL -> if (isSpanish) "Otoño" else "Fall"
    }

    val years = (currentSeasonAndYear().year + 1 downTo 2000).toList()

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
                label = if (isSpanish) "Temporada" else "Season",
                selectedOption = stateHolder.season,
                options = seasons,
                optionLabel = { it.label() },
                onSelect = { stateHolder.selectSeason(it) },
            )
            PlatformFilterChip(
                label = if (isSpanish) "Año" else "Year",
                selectedOption = stateHolder.year,
                options = years,
                optionLabel = { it.toString() },
                onSelect = { stateHolder.selectYear(it) },
            )
            val (currentSortField, currentDescending) = parseDiscoverSort(stateHolder.sort)
            val selectedSortField = sortFields.firstOrNull { it.field == currentSortField } ?: sortFields.first()
            PlatformFilterChip(
                label = if (isSpanish) "Orden" else "Sort",
                selectedOption = selectedSortField,
                options = sortFields,
                optionLabel = { it.label },
                onSelect = { field ->
                    stateHolder.selectSort(combineDiscoverSort(field.field, currentDescending))
                },
            )
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when {
                stateHolder.isLoading && stateHolder.results.isEmpty() ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                stateHolder.error != null && stateHolder.results.isEmpty() ->
                    SeasonalCenteredMessage(text = stateHolder.error ?: "", isError = true)
                stateHolder.results.isEmpty() ->
                    SeasonalCenteredMessage(
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
                        items(stateHolder.results, key = { it.id }) { result ->
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
        val (currentSortFieldForSheet, currentDescendingForSheet) = parseDiscoverSort(stateHolder.sort)
        DiscoverFilterSheet(
            title = if (isSpanish) "Filtros" else "Filters",
            orderTitle = if (isSpanish) "Dirección" else "Order",
            ascendingLabel = if (isSpanish) "Ascendente" else "Ascending",
            descendingLabel = if (isSpanish) "Descendente" else "Descending",
            currentDescending = currentDescendingForSheet,
            showOnListFilters = true,
            hideOnListLabel = if (isSpanish) "Ocultar las de mi lista" else "Hide series on my list",
            onlyOnListLabel = if (isSpanish) "Solo las de mi lista" else "Only show series on my list",
            currentOnList = stateHolder.onList,
            applyLabel = if (isSpanish) "Aplicar" else "Apply",
            onDismiss = onDismissSortSheet,
            onApply = { descending, onList ->
                stateHolder.applySortAndOnList(combineDiscoverSort(currentSortFieldForSheet, descending), onList)
                onDismissSortSheet()
            },
        )
    }
}

@Composable
private fun SeasonalCenteredMessage(text: String, isError: Boolean = false) {
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
