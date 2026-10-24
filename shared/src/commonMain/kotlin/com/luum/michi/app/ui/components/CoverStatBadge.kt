package com.luum.michi.app.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

/** One stat of a card's chip row: an icon plus a short, already-formatted value. */
internal data class CoverStat(
    val icon: ImageVector,
    val value: String,
)
