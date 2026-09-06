package com.luum.michi.app.discover.presentation.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformChipRow
import com.luum.michi.app.core.platform.components.PlatformFilterOption
import com.luum.michi.app.core.platform.components.PlatformModalSheet
import com.luum.michi.app.core.platform.components.PlatformSheetActionBar
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.domain.model.exploreFormats
import com.luum.michi.app.discover.domain.model.exploreGenres
import com.luum.michi.app.discover.domain.model.exploreSeasonOptions
import com.luum.michi.app.discover.domain.model.exploreYears
import com.luum.michi.app.discover.domain.model.filterLabel
import com.luum.michi.app.discover.presentation.explore.state.ExploreStateHolder

/**
 * Filtro de Explore con borrador local: los chips editan el draft y solo
 * "Aplicar" dispara [ExploreStateHolder.updateFilters] (con su debounce).
 * "Restablecer" limpia el draft sin tocar el holder.
 */
@Composable
internal fun ExploreFilterSheet(
    stateHolder: ExploreStateHolder,
    onDismiss: () -> Unit,
) {
    val strings = LanguageProvider.strings
    var draftCategory by remember { mutableStateOf(stateHolder.category) }
    var draftSeason by remember { mutableStateOf(stateHolder.season) }
    var draftGenres by remember { mutableStateOf(stateHolder.genres) }
    var draftFormats by remember { mutableStateOf(stateHolder.formats) }
    var draftYear by remember { mutableStateOf(stateHolder.year) }

    val draftIsEntity = draftCategory == ExploreCategory.CHARACTERS ||
        draftCategory == ExploreCategory.STAFF ||
        draftCategory == ExploreCategory.STUDIOS
    val typeOptions = remember(strings) {
        ExploreCategory.entries.map { PlatformFilterOption(it.name, it.filterLabel(strings)) }
    }
    val seasonOptions = remember(strings) { exploreSeasonOptions(strings) }
    val genreOptions = remember { exploreGenres().map { PlatformFilterOption(it, it) } }
    val formatOptions = remember {
        exploreFormats().map { PlatformFilterOption(it.value, it.label) }
    }
    val yearOptions = remember(strings) {
        exploreYears().map { PlatformFilterOption(it?.toString() ?: "any", it?.toString() ?: strings.exploreAnyYearLabel) }
    }

    PlatformModalSheet(onDismiss = onDismiss) { modifier ->
        Column(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.filterByLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                item(key = "group_type") {
                    FilterGroupSection(title = strings.exploreFilterTypeLabel) {
                        PlatformChipRow(
                            options = typeOptions,
                            selectedIds = setOf(draftCategory.name),
                            onToggle = { id ->
                                ExploreCategory.entries.firstOrNull { it.name == id }?.let {
                                    draftCategory = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (!draftIsEntity) {
                    if (draftCategory == ExploreCategory.ANIME) {
                        item(key = "group_season") {
                            FilterGroupSection(title = strings.exploreFilterSeasonLabel) {
                                PlatformChipRow(
                                    options = seasonOptions,
                                    selectedIds = setOf(draftSeason ?: "any"),
                                    onToggle = { id -> draftSeason = if (id == "any") null else id },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    item(key = "group_genre") {
                        FilterGroupSection(title = strings.exploreFilterGenreLabel) {
                            PlatformChipRow(
                                options = genreOptions,
                                selectedIds = draftGenres.toSet(),
                                onToggle = { id ->
                                    draftGenres = if (id in draftGenres) draftGenres - id else draftGenres + id
                                },
                                multiSelect = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    item(key = "group_format") {
                        FilterGroupSection(title = strings.exploreFilterFormatLabel) {
                            PlatformChipRow(
                                options = formatOptions,
                                selectedIds = draftFormats.toSet(),
                                onToggle = { id ->
                                    draftFormats = if (id in draftFormats) draftFormats - id else draftFormats + id
                                },
                                multiSelect = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    item(key = "group_year") {
                        FilterGroupSection(title = strings.exploreFilterYearLabel) {
                            PlatformChipRow(
                                options = yearOptions,
                                selectedIds = setOf(draftYear?.toString() ?: "any"),
                                onToggle = { id -> draftYear = if (id == "any") null else id.toIntOrNull() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            PlatformSheetActionBar(
                leadingLabel = strings.filterResetAction,
                trailingLabel = strings.filterApplyAction,
                onLeadingClick = {
                    draftCategory = ExploreCategory.ANIME
                    draftSeason = null
                    draftGenres = emptyList()
                    draftFormats = emptyList()
                    draftYear = null
                },
                onTrailingClick = {
                    stateHolder.updateFilters(
                        newCategory = draftCategory,
                        newSeason = if (draftCategory == ExploreCategory.ANIME) draftSeason else null,
                        newGenres = draftGenres,
                        newFormats = draftFormats,
                        newYear = draftYear,
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FilterGroupSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}
