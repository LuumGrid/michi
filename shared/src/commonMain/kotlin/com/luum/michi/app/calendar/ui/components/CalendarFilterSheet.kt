package com.luum.michi.app.calendar.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.calendar.domain.model.CalendarSeasonFilter
import com.luum.michi.app.calendar.domain.model.CalendarStatusFilter
import com.luum.michi.app.calendar.domain.model.label
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.GhostButton
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionList
import com.luum.michi.app.ui.language.Strings

/**
 * Calendar filter sheet: season + status single-select groups over
 * shared [OptionRow]s (both filters are single-choice, so no checkbox
 * rows needed). Applies immediately on every tap; reset restores ALL.
 */
@Composable
internal fun CalendarFilterSheet(
    season: CalendarSeasonFilter,
    onSelectSeason: (CalendarSeasonFilter) -> Unit,
    status: CalendarStatusFilter,
    onSelectStatus: (CalendarStatusFilter) -> Unit,
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
            GhostButton(
                label = strings.filterResetAction,
                onClick = onReset,
            )
        },
    ) {
        OptionList(
            options = CalendarSeasonFilter.entries,
            selected = season,
            label = { it.label(strings) },
            onSelect = { onSelectSeason(it) },
            title = strings.seasonLabel,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OptionList(
            options = CalendarStatusFilter.entries,
            selected = status,
            label = { it.label(strings) },
            onSelect = { onSelectStatus(it) },
            title = strings.statusLabel,
        )
    }
}
