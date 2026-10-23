package com.luum.michi.app.ui.components

import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.core.model.compactCount
import com.luum.michi.app.ui.icons.AppIcons

/**
 * Stat chips with one format everywhere: the score reads `85%` and counts use
 * `core/model`'s `compactCount()` (`1.8K`). Null paints nothing, so a caller can
 * just list the candidates it has. Shared by the card rails.
 */
internal fun scoreBadge(score: Int?): CoverStat? =
    score?.let { CoverStat(AppIcons.StarOutline, "$it%") }

internal fun popularityBadge(count: Int?): CoverStat? =
    count?.let { CoverStat(AppIcons.Groups, it.compactCount()) }

/**
 * Poster-grid item for a catalog work (studio/genre/catalog grids): 2-genre
 * subtitle and the palette placeholder — the shape [DetailPosterGrid] paints.
 */
internal fun MediaGridItem.toPosterItem(): MediaRailItem = MediaRailItem(
    id = mediaId,
    title = title,
    image = cover,
    subtitle = genres.take(2).joinToString(" · ").ifBlank { null },
    placeholderHex = paletteHex,
)
