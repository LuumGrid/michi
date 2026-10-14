package com.luum.michi.app.discover.domain.model
import com.luum.michi.app.core.model.CoverSizes

/**
 * A dashboard rail entry. Only what the rail renders: id/title/cover + the
 * preformatted `meta` line, plus the raw stats the rail badges may show (the
 * screen decides which ones per [DashboardSectionKind.cardStats]). The dashboard
 * never shows the viewer entry, so the query does not fetch it.
 */
internal data class MediaItem(
    val title: String,
    val meta: String,
    val id: Int? = null,
    val cover: CoverSizes? = null,
    /** Cover color: paints the card while the image loads. */
    val placeholderHex: String? = null,
    val averageScore: Int? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
)
