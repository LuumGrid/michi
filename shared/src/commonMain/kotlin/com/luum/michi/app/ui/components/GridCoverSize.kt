package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Minimum air a poster grid keeps between columns and edges. */
private val MinGridGap = 12.dp

/**
 * How much a grid cell may grow past its base token. A grid starts from the
 * token and only absorbs leftover air up to +25%; past that cap the surplus goes
 * back to even air (`Arrangement.SpaceEvenly`) instead of one enormous cell.
 */
private const val GridMaxGrowth = 1.25f

/**
 * The cover size a poster grid uses. It always starts from a standard token —
 * [CoverSize.Featured] when [minColumns] of them fit with [MinGridGap] between
 * and around, otherwise the next token down ([CoverSize.Rail]) — and then grows
 * the cell to fill the width, never past [GridMaxGrowth]. So the grid never
 * invents a size (the width and the 3:4 height both derive from the token) and
 * never leaves a huge margin: whatever is left after the cap is spread as air.
 */
internal fun gridCoverSize(availableWidth: Dp, minColumns: Int = 2): CoverSize {
    val base = if (availableWidth >= CoverSize.Featured.width * minColumns + MinGridGap * (minColumns + 1)) {
        CoverSize.Featured
    } else {
        CoverSize.Rail
    }
    // How many base cells fit with a gap between and around them.
    val columns = ((availableWidth - MinGridGap) / (base.width + MinGridGap))
        .toInt()
        .coerceAtLeast(minColumns)
    val cellWidth = (availableWidth - MinGridGap * (columns + 1)) / columns
    return CoverSize(cellWidth.coerceIn(base.width, base.width * GridMaxGrowth))
}

/**
 * Measures the width the grid gets and hands [gridCoverSize]'s size to
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
