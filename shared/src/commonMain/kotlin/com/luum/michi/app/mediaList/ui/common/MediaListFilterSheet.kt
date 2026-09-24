package com.luum.michi.app.mediaList.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.FilterOption
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.OptionRow
import com.luum.michi.app.ui.language.Strings

/**
 * Shared list filter sheet: season + year single-select, genres + formats
 * multi-select. Primitives in, callbacks out — the screen maps holder state
 * (see Anime/MangaListScreen) so this file never touches a holder or a
 * concrete entry. Applies immediately on every tap, like the Settings
 * pickers; [onReset] clears all four groups at once.
 */
@Composable
internal fun MediaListFilterSheet(
    seasonOptions: List<FilterOption>,
    selectedSeasonId: String,
    onSelectSeason: (String) -> Unit,
    yearOptions: List<Int?>,
    selectedYear: Int?,
    onSelectYear: (Int?) -> Unit,
    genres: List<String>,
    selectedGenres: List<String>,
    onToggleGenre: (String) -> Unit,
    formatOptions: List<FilterOption>,
    selectedFormatIds: List<String>,
    onToggleFormat: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
) {
    ModalSheet(
        title = strings.filterAction,
        dismissLabel = strings.dismissAction,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        OptionGroup(title = strings.seasonLabel) {
            seasonOptions.forEachIndexed { index, option ->
                OptionRow(
                    label = option.label,
                    selected = option.id == selectedSeasonId,
                    onClick = { onSelectSeason(option.id) },
                    divider = index != 0,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OptionGroup(title = strings.exploreFilterYearLabel) {
            // "Any" stays a lone top row; the rest groups visually by
            // decade (headers only — selection is still the exact year,
            // so the filter predicate is untouched).
            if (yearOptions.any { it == null }) {
                OptionRow(
                    label = strings.exploreAnyYearLabel,
                    selected = selectedYear == null,
                    onClick = { onSelectYear(null) },
                    divider = false,
                )
            }
            yearOptions.filterNotNull()
                .groupBy { it / 10 * 10 }
                .toSortedMap(compareByDescending { it })
                .forEach { (decade, years) ->
                    DecadeHeader(label = "$decade–${decade + 9}")
                    years.forEach { year ->
                        OptionRow(
                            label = year.toString(),
                            selected = year == selectedYear,
                            onClick = { onSelectYear(year) },
                            divider = true,
                        )
                    }
                }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OptionGroup(title = strings.exploreFilterGenreLabel) {
            genres.forEachIndexed { index, genre ->
                FilterCheckRow(
                    label = genre,
                    checked = genre in selectedGenres,
                    onToggle = { onToggleGenre(genre) },
                    divider = index != 0,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OptionGroup(title = strings.formatLabel) {
            formatOptions.forEachIndexed { index, option ->
                FilterCheckRow(
                    label = option.label,
                    checked = option.id in selectedFormatIds,
                    onToggle = { onToggleFormat(option.id) },
                    divider = index != 0,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = strings.filterResetAction,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * Visual-only decade separator inside the year group (same label style as
 * [com.luum.michi.app.ui.components.OptionGroup] titles, tighter spacing
 * since it lives inside the frame).
 */
@Composable
private fun DecadeHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}
