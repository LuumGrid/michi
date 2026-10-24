package com.luum.michi.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Section header shared by the detail pages and the media rails. No padding
 * of its own: callers own the horizontal inset so panel and rail headers stay
 * aligned with their content.
 */
@Composable
internal fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}
