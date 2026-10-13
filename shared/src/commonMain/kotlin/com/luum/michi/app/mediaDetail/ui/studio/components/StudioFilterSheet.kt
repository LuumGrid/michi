package com.luum.michi.app.mediaDetail.ui.studio.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.studio.model.StudioMediaSort
import com.luum.michi.app.mediaDetail.domain.studio.model.label
import com.luum.michi.app.mediaDetail.ui.studio.state.StudioDetailStateHolder
import com.luum.michi.app.ui.components.GhostButton
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.OptionRow
import com.luum.michi.app.ui.components.SheetActionBar
import com.luum.michi.app.ui.language.Strings

/** Sorts the studio sheet offers (the rest keep the server order, so they would not change anything). */
private val StudioSortOptions = listOf(
    StudioMediaSort.POPULARITY,
    StudioMediaSort.SCORE,
    StudioMediaSort.NEWEST,
    StudioMediaSort.OLDEST,
)

/**
 * Studio sort / type / format picker, opened from the toolbar filter action
 * or the grid's summary line. Every row applies on tap (the list is already
 * loaded; nothing re-queries), Reset restores sort and filters. The format
 * group only exists when the loaded media has formats to pick from.
 */
@Composable
internal fun StudioFilterSheet(
    holder: StudioDetailStateHolder,
    onDismiss: () -> Unit,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
) {
    val formats = holder.availableFormats
    ModalSheet(
        title = strings.filterAction,
        dismissLabel = strings.dismissAction,
        onDismiss = onDismiss,
        modifier = modifier,
        footer = {
            SheetActionBar {
                GhostButton(
                    label = strings.filterResetAction,
                    onClick = { holder.resetFilters() },
                )
            }
        },
    ) {
        OptionGroup(title = strings.sortAction) {
            StudioSortOptions.forEachIndexed { index, option ->
                OptionRow(
                    label = option.label(strings),
                    selected = option == holder.sort,
                    onClick = { holder.changeSort(option) },
                    divider = index != 0,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OptionGroup(title = strings.exploreFilterTypeLabel) {
            studioTypeOptions(strings).forEachIndexed { index, (value, label) ->
                OptionRow(
                    label = label,
                    selected = value == holder.typeFilter,
                    onClick = { holder.changeTypeFilter(value) },
                    divider = index != 0,
                )
            }
        }
        if (formats.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            OptionGroup(title = strings.mediaDetailFormatLabel) {
                (listOf<String?>(null) + formats).forEachIndexed { index, value ->
                    OptionRow(
                        label = value ?: strings.sectionAll,
                        selected = value == holder.formatFilter,
                        onClick = { holder.changeFormatFilter(value) },
                        divider = index != 0,
                    )
                }
            }
        }
    }
}

/** One-line state of the sheet for the grid header: "Popularity · Anime · TV". */
internal fun studioFilterSummary(holder: StudioDetailStateHolder, strings: LanguageStrings): String =
    listOfNotNull(
        holder.sort.label(strings),
        holder.typeFilter?.let { type -> studioTypeOptions(strings).firstOrNull { it.first == type }?.second },
        holder.formatFilter,
    ).joinToString(" · ")

private fun studioTypeOptions(strings: LanguageStrings): List<Pair<String?, String>> = listOf(
    null to strings.sectionAll,
    "ANIME" to strings.animeLabel,
    "MANGA" to strings.mangaLabel,
)
