package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Fixed action row for bottom sheets: caller-owned buttons (Reset alone
 * today; side-by-side pairs like Reset + Apply or Cancel + Save tomorrow,
 * with weight(1f) each). Rendered in [ModalSheet]'s footer slot, so it
 * never scrolls away. Deliberately capsule-free: the glass buttons carry
 * their own container, and a capsule around them draws a double frame.
 */
@Composable
internal fun SheetActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}
