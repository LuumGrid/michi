package com.luum.michi.app.mediaDetail.ui.media.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.medialist.domain.MediaListStatus
import com.luum.michi.app.ui.components.ChipPill

/**
 * Single-select status chips in a horizontal rail (the editor goes rail,
 * not rows, for the 6 statuses). Plain Row + manual scroll on purpose:
 * a LazyRow nested in the sheet's scroll column mismeasures (overlapping
 * rows); this one sizes itself like any other row. No auto-scroll (the
 * first chips are always visible; the rest are tapped in view) and no
 * counts (unlike the list section rails) — selection is the only state.
 */
@Composable
internal fun StatusChipRail(
    options: List<MediaListStatus>,
    labels: List<String>,
    selected: MediaListStatus,
    onSelect: (MediaListStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, option ->
            ChipPill(
                label = labels[index],
                selected = option == selected,
                onClick = { onSelect(option) },
            )
        }
    }
}
