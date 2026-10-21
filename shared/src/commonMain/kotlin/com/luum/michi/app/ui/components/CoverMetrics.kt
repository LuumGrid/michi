package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A standard poster size: the width comes from the token and the height is its
 * 3:4, so **both** dimensions come from one place and no surface can invent a
 * size. Every cover in the app is drawn from one of [List], [Compact], [Rail]
 * or [Featured] — a poster grid takes one as its base and grows the cell up to
 * +25% to fill the width (`gridCoverSize`), never inventing a size of its own.
 *
 * - [List]: a row cover — it shares the row with text (lists, calendar, "View
 *   all" rows) and leads a one-row card rail.
 * - [Compact]: the short cover inside a three-row card rail, where the cover
 *   shares a 120dp card with a one-line title, the meta line and a chip.
 * - [Rail]: a poster rail card, where the cover **is** the card (detail and
 *   people rails) — the middle step, exactly halfway between [List] and
 *   [Featured].
 * - [Featured]: the largest standard cover (the media and person heroes) and
 *   the base of a grid cell (grown up to +25% by `gridCoverSize`), so grids
 *   reuse it instead of adding another size.
 */
internal data class CoverSize(val width: Dp) {
    /** The other dimension, derived: a poster is always 3:4. */
    val height: Dp get() = width * 4 / 3

    companion object {
        val List = CoverSize(111.dp)
        val Compact = CoverSize(72.dp)
        val Featured = CoverSize(152.dp)

        /** 131.5dp: halfway between the row cover and the featured one. */
        val Rail = CoverSize((List.width + Featured.width) / 2)
    }
}

/** Sizes a poster exactly like the token: width and height, never one alone. */
internal fun Modifier.coverSize(size: CoverSize): Modifier =
    this.width(size.width).height(size.height)

/**
 * How far the hero's cover rises above the banner's bottom edge: half the
 * [CoverSize.Featured] height, so the poster hangs into the content below
 * instead of floating dead-center over the backdrop.
 */
internal val HeroCoverOverlap = CoverSize.Featured.height / 2

/**
 * The standard avatar diameters: a settings/list **row** avatar, and the larger
 * **header** avatar (the profile card). Tokens so no surface invents a size.
 */
internal object AvatarSize {
    val Row = 48.dp
    val Header = 64.dp
}
