package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Section header shared by every detail scope (media, character, staff,
 * studio). No padding of its own: callers own the horizontal inset so the
 * panel and rail headers stay aligned with their content.
 */
@Composable
internal fun MediaDetailSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}
