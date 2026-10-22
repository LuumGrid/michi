package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.icons.AppIcons

/** Baked 16dp matching the full-width layout (panels are already inset). */
internal val RailHorizontalPadding = 16.dp

/**
 * The one rail header: a [SectionHeader] with an optional trailing control on
 * the same row, carrying the standard rail insets. Level-1 rails ([MediaCardRail]),
 * the profile's studio-favourites section and the media tags section draw it, so
 * a section header measures the same wherever it appears.
 *
 * [viewAllLabel]/[onViewAll] are the standard "View all" pill. [trailing] is a
 * generic slot for any other tool (e.g. the tags spoiler toggle); when both are
 * supplied, [trailing] wins.
 */
@Composable
internal fun RailHeader(
    title: String,
    modifier: Modifier = Modifier,
    viewAllLabel: String? = null,
    onViewAll: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = RailHorizontalPadding, end = RailHorizontalPadding, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeader(title = title, modifier = Modifier.weight(1f))
        when {
            trailing != null -> trailing()
            onViewAll != null && viewAllLabel != null -> TrailingPill(
                label = viewAllLabel,
                onClick = onViewAll,
                trailingIcon = AppIcons.ChevronRight,
            )
        }
    }
}
