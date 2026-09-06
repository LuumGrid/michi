package com.luum.michi.app.mediaList.presentation.common

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

/**
 * Filtro de las listas (Anime/Manga), espejo del de Explore: todo edita un
 * borrador local y solo "Aplicar" confirma en el holder ("Restablecer" limpia
 * el borrador). Los botones gobiernan cuándo se aplica el filtrado, igual que
 * en Explore. El filtrado es client-side (la colección completa ya está
 * cargada), así que no hay debounce ni red aquí.
 */
@Composable
internal fun MediaListFilterSheet(
    sectionOptions: List<PlatformFilterOption>,
    initialSectionId: String,
    showSeasonFilter: Boolean,
    isAnimeTab: Boolean,
    season: String?,
    genres: List<String>,
    formats: List<String>,
    year: Int?,
    onApply: (
        sectionId: String,
        season: String?,
        genres: List<String>,
        formats: List<String>,
        year: Int?,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LanguageProvider.strings
    var draftSection by remember { mutableStateOf(initialSectionId) }
    var draftSeason by remember { mutableStateOf(season) }
    var draftGenres by remember { mutableStateOf(genres) }
    var draftFormats by remember { mutableStateOf(formats) }
    var draftYear by remember { mutableStateOf(year) }

    val seasonOptions = remember(strings) { mediaListSeasonOptions(strings) }
    val genreOptions = remember { mediaListGenres().map { PlatformFilterOption(it, it) } }
    val formatOptions = remember(isAnimeTab) { mediaListFormatOptions(isAnimeTab) }
    val yearOptions = remember(strings) {
        mediaListYears().map { PlatformFilterOption(it?.toString() ?: "any", it?.toString() ?: strings.exploreAnyYearLabel) }
    }

    PlatformModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.65f,
    ) { modifier ->
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
                item(key = "group_section") {
                    MediaListFilterGroupSection(title = strings.statusLabel) {
                        PlatformChipRow(
                            options = sectionOptions,
                            selectedIds = setOf(draftSection),
                            onToggle = { draftSection = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (showSeasonFilter) {
                    item(key = "group_season") {
                        MediaListFilterGroupSection(title = strings.exploreFilterSeasonLabel) {
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
                    MediaListFilterGroupSection(title = strings.exploreFilterGenreLabel) {
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
                    MediaListFilterGroupSection(title = strings.exploreFilterFormatLabel) {
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
                    MediaListFilterGroupSection(title = strings.exploreFilterYearLabel) {
                        PlatformChipRow(
                            options = yearOptions,
                            selectedIds = setOf(draftYear?.toString() ?: "any"),
                            onToggle = { id -> draftYear = if (id == "any") null else id.toIntOrNull() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            PlatformSheetActionBar(
                leadingLabel = strings.filterResetAction,
                trailingLabel = strings.filterApplyAction,
                onLeadingClick = {
                    draftSection = sectionOptions.firstOrNull()?.id ?: draftSection
                    draftSeason = null
                    draftGenres = emptyList()
                    draftFormats = emptyList()
                    draftYear = null
                },
                onTrailingClick = {
                    onApply(draftSection, draftSeason, draftGenres, draftFormats, draftYear)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MediaListFilterGroupSection(
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
