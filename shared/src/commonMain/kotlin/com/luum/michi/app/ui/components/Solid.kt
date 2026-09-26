package com.luum.michi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Solid surface recipe: opaque container + full-strength outline border.
 * Twin of the glass recipe in `Glass.kt` — same metrics everywhere, only
 * the recipe varies, so branches always read
 * `if (glass) glassX() else solidX()`.
 */
@Composable
internal fun solidContainerColor(): Color =
    MaterialTheme.colorScheme.surface

@Composable
internal fun solidBorder(): BorderStroke =
    BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
