package com.luum.michi.app.calendar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.ui.components.GlassCapsule
import com.luum.michi.app.ui.components.GlassCircleButton
import com.luum.michi.app.ui.icons.AppIcons

/**
 * Bottom date bar: previous-day chevron, date button (opens the system
 * calendar), next-day chevron. Same floating full-width capsule language
 * as the [TabBar]: centered box, 16dp sides, 24dp bottom margin, content
 * scrolls behind it. The date cell is caller-owned (the picker field
 * hosts its own dialog, so it arrives as a slot).
 */
@Composable
internal fun CalendarDateBar(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    modifier: Modifier = Modifier,
    dateContent: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        GlassCapsule(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircleButton(
                    icon = AppIcons.Back,
                    contentDescription = null,
                    onClick = onPrevious,
                    enabled = previousEnabled,
                )
                dateContent()
                GlassCircleButton(
                    icon = AppIcons.ChevronRight,
                    contentDescription = null,
                    onClick = onNext,
                    enabled = nextEnabled,
                )
            }
        }
    }
}
