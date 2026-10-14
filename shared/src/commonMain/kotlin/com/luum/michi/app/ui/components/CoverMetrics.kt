package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A standard poster size: the width comes from the token and the height is its
 * 3:4, so **both** dimensions come from one place and no surface can invent a
 * size. Every cover in the app is drawn from one of [List], [Rail] or
 * [Featured].
 *
 * - [List]: a row cover — it shares the row with text (lists, calendar, "View
 *   all" rows).
 * - [Rail]: a rail card, where the cover **is** the card (dashboard and detail
 *   rails) — the middle step, exactly halfway between [List] and [Featured].
 * - [Featured]: leads the dashboard, is the largest standard cover (the media
 *   and person heroes use it too) and sizes a grid cell, so grids reuse it
 *   instead of adding a fourth size.
 */
internal data class CoverSize(val width: Dp) {
    /** The other dimension, derived: a poster is always 3:4. */
    val height: Dp get() = width * 4 / 3

    companion object {
        val List = CoverSize(111.dp)
        val Featured = CoverSize(152.dp)

        /** 131.5dp: halfway between the row cover and the featured one. */
        val Rail = CoverSize((List.width + Featured.width) / 2)
    }
}

/** Sizes a poster exactly like the token: width and height, never one alone. */
internal fun Modifier.coverSize(size: CoverSize): Modifier =
    this.width(size.width).height(size.height)
