package com.luum.michi.app.ui.components

import com.luum.michi.app.core.model.CoverSizes

/** One item of a [MediaCardRail] or a poster grid: cover + title, plus an
 *  optional second line and accent. */
internal data class MediaRailItem(
    val id: Int,
    val title: String,
    val image: CoverSizes?,
    val subtitle: String? = null,
    // Extra catalog line: the "View all" rows take it as the subtitle and the
    // card rail paints it as the subtitle when present.
    val meta: String? = null,
    // Explicit text chip for the card rail; null = no chip. The rail never
    // derives a chip from meta or subtitle.
    val tag: String? = null,
    // Stable identity for LazyRow. Defaults to the id, but callers whose id
    // can repeat (staff with multiple roles) pass a unique edge key.
    val key: String = "",
    /** Cover color painted while the image loads (dashboard rails). */
    val placeholderHex: String? = null,
    // Up to two badges painted over the cover (first = top-start). Empty keeps
    // every existing rail byte-identical.
    val stats: List<CoverStat> = emptyList(),
)
