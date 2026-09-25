package com.luum.michi.app.mediaList.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.FilterOption
import com.luum.michi.app.ui.components.GhostButton
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.OptionRow
import com.luum.michi.app.ui.components.SheetActionBar
import com.luum.michi.app.ui.icons.AppIcons
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
    yearTabs: List<MediaListSectionTab<Int?>>,
    selectedYear: Int?,
    onSelectYear: (Int?) -> Unit,
    expandedDecade: Int?,
    onToggleDecade: (Int) -> Unit,
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
        footer = {
            SheetActionBar {
                GhostButton(
                    label = strings.filterResetAction,
                    onClick = onReset,
                )
            }
        },
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
        // Accordion year group (variant C): decade rows expand inline to
        // their year rail. Tabs arrive flat and group here, so screens and
        // the filter predicate stay untouched; selection is the exact year.
        OptionGroup(title = strings.exploreFilterYearLabel) {
            var firstRow = true
            if (yearTabs.any { it.value == null }) {
                OptionRow(
                    label = strings.exploreAnyYearLabel,
                    selected = selectedYear == null,
                    onClick = { onSelectYear(null) },
                    divider = false,
                )
                firstRow = false
            }
            yearTabs.filter { it.value != null }
                .groupBy { it.value!! / 10 * 10 }
                .toSortedMap(compareByDescending { it })
                .forEach { (decade, tabs) ->
                    val expanded = decade == expandedDecade
                    val decadeSelected = tabs.any { it.value == selectedYear }
                    OptionRow(
                        label = "$decade–${decade + 9}",
                        selected = decadeSelected,
                        onClick = { onToggleDecade(decade) },
                        divider = !firstRow,
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (expanded) {
                                        AppIcons.ExpandLess
                                    } else {
                                        AppIcons.ExpandMore
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Checkbox(
                                    checked = decadeSelected,
                                    onCheckedChange = null,
                                )
                            }
                        },
                    )
                    firstRow = false
                    if (expanded) {
                        MediaListSectionRail(
                            tabs = tabs,
                            selected = selectedYear,
                            onSelect = { onSelectYear(it) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.padding(bottom = 8.dp),
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
    }
}
