package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Minimum air a poster grid keeps between columns and edges. */
private val MinGridGap = 12.dp

/**
 * The cover token a poster grid uses, so its cells are exactly a standard
 * poster and the leftover becomes even air: [CoverSize.Featured] when
 * [minColumns] of them fit with [MinGridGap] between and around, otherwise the
 * next standard token down ([CoverSize.Rail]). The cell, the poster and the
 * image request all use the returned token — a grid never stretches a cover.
 */
internal fun gridCoverSize(availableWidth: Dp, minColumns: Int = 2): CoverSize =
    if (availableWidth >= CoverSize.Featured.width * minColumns + MinGridGap * (minColumns + 1)) {
        CoverSize.Featured
    } else {
        CoverSize.Rail
    }

/**
 * Measures the width the grid gets and hands [gridCoverSize]'s token to
 * [content], so no screen repeats the measurement.
 */
@Composable
internal fun GridCoverSizeProvider(
    minColumns: Int = 2,
    content: @Composable (CoverSize) -> Unit,
) {
    BoxWithConstraints {
        content(gridCoverSize(availableWidth = maxWidth, minColumns = minColumns))
    }
}
