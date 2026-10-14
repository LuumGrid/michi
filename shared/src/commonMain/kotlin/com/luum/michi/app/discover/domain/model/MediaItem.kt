package com.luum.michi.app.discover.domain.model
import com.luum.michi.app.core.model.CoverSizes

/**
 * A dashboard rail entry. Only what the rail renders: id/title/cover + the
 * preformatted `meta` line. The dashboard never shows scores, favourites or
 * the viewer entry, so the query does not fetch them.
 */
internal data class MediaItem(
    val title: String,
    val meta: String,
    val id: Int? = null,
    val cover: CoverSizes? = null,
    /** Cover color: paints the card while the image loads. */
    val placeholderHex: String? = null,
)
