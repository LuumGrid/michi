package com.luum.michi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Flat surface recipe: opaque container + full-strength outline border. Twin of
 * the glass recipe in `Glass.kt` — same metrics everywhere. Content (cards,
 * panels, groups, sheet fields) always uses this, whether or not it is tappable;
 * only floating tools may branch to glass (see `toolContainerColor`/`toolBorder`).
 */
@Composable
internal fun solidContainerColor(): Color =
    MaterialTheme.colorScheme.surface

@Composable
internal fun flatBorder(): BorderStroke =
    BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
