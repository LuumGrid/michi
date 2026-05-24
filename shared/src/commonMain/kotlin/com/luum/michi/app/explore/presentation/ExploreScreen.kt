package com.luum.michi.app.explore.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformChips
import com.luum.michi.app.explore.presentation.state.ExploreCategory
import com.luum.michi.app.explore.presentation.state.ExploreStateHolder
import com.luum.michi.app.search.presentation.components.SearchResultCard

@Composable
internal fun ExploreScreen(
    stateHolder: ExploreStateHolder,
    showFilters: Boolean,
    onShowFiltersChange: (Boolean) -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings
    val isSpanish = strings.languageLabel.equals("Idioma", ignoreCase = true)

    androidx.compose.runtime.LaunchedEffect(stateHolder) {
        if (stateHolder.results.isEmpty() && !stateHolder.isLoading) {
            stateHolder.load()
        }
    }

    // 1. Categories
    val categories = listOf(
        ExploreCategory.ANIMATION,
        ExploreCategory.READING,
        ExploreCategory.CHARACTERS,
        ExploreCategory.STAFF,
        ExploreCategory.STUDIOS
    )

    fun ExploreCategory.label(): String = when (this) {
        ExploreCategory.ANIMATION -> if (isSpanish) "Animación" else "Animation"
        ExploreCategory.READING -> if (isSpanish) "Lectura" else "Reading"
        ExploreCategory.CHARACTERS -> if (isSpanish) "Personajes" else "Characters"
        ExploreCategory.STAFF -> "Staff"
        ExploreCategory.STUDIOS -> if (isSpanish) "Estudios" else "Studios"
    }

    // 2. Genres options
    val allLabel = if (isSpanish) "Todos" else "All"
    val genres = listOf(
        allLabel, "Action", "Adventure", "Comedy", "Drama", "Fantasy",
        "Horror", "Mecha", "Music", "Mystery", "Romance", "Sci-Fi",
        "Slice of Life", "Sports", "Supernatural", "Thriller"
    )

    // 3. Formats options
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
            FormatOption("ONE_SHOT", "One-shots")
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
            FormatOption("ONE_SHOT", "One-shots")
        )
    }
    val currentFormatOption = formatOptions.find { it.value.equals(stateHolder.format, ignoreCase = true) } ?: formatOptions.first()

    // 4. Sort options
    val sortOptions = if (isSpanish) {
        listOf(
            SortOption("POPULARITY_DESC", "Más popular"),
            SortOption("SCORE_DESC", "Mejor puntaje"),
            SortOption("TRENDING_DESC", "Tendencia"),
            SortOption("START_DATE_DESC", "Recientes")
        )
    } else {
        listOf(
            SortOption("POPULARITY_DESC", "Most Popular"),
            SortOption("SCORE_DESC", "Top Rated"),
            SortOption("TRENDING_DESC", "Trending"),
            SortOption("START_DATE_DESC", "Latest")
        )
    }
    val currentSortOption = sortOptions.find { it.value == stateHolder.sort } ?: sortOptions.first()

    // 5. Year options
    val yearOptions = listOf(null, 2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018, 2017, 2016, 2015, 2014, 2013, 2012, 2011, 2010, 2005, 2000)

    Column(modifier = Modifier.fillMaxSize()) {
        // Category chips: Animation, Reading, Characters, Staff, Studios
        PlatformChips(
            items = categories,
            selectedItem = stateHolder.category,
            onSelect = {
                stateHolder.updateFilters(newCategory = it)
                // Auto collapse filters when selecting a category that doesn't support them
                if (it == ExploreCategory.CHARACTERS || it == ExploreCategory.STAFF || it == ExploreCategory.STUDIOS) {
                    onShowFiltersChange(false)
                }
            },
            label = { it.label() },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        // Expandable advanced filter panel for Anime & Manga
        AnimatedVisibility(
            visible = showFilters && !stateHolder.isEntitySearch(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                // Genre chips
                PlatformChips(
                    items = genres,
                    selectedItem = stateHolder.genre,
                    onSelect = { stateHolder.updateFilters(newGenre = it) },
                    label = { if (it == "All" || it == "Todos") allLabel else it },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Format chips
                PlatformChips(
                    items = formatOptions,
                    selectedItem = currentFormatOption,
                    onSelect = { stateHolder.updateFilters(newFormat = it.value) },
                    label = { it.label },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Sort chips
                PlatformChips(
                    items = sortOptions,
                    selectedItem = currentSortOption,
                    onSelect = { stateHolder.updateFilters(newSort = it.value) },
                    label = { it.label },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Year chips
                PlatformChips(
                    items = yearOptions,
                    selectedItem = stateHolder.year,
                    onSelect = { stateHolder.updateFilters(newYear = it) },
                    label = { it?.toString() ?: (if (isSpanish) "Cualquier año" else "Any year") },
                    modifier = Modifier.fillMaxWidth(),
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
                    CenteredMessage(text = stateHolder.error ?: "", isError = true)
                stateHolder.results.isEmpty() ->
                    CenteredMessage(
                        text = if (isSpanish) {
                            "No se encontraron resultados en el catálogo."
                        } else {
                            "No catalog results found."
                        }
                    )
                else -> LazyVerticalGrid(
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
                }
            }
        }
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
private data class SortOption(val value: String, val label: String)
