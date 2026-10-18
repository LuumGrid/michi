package com.luum.michi.app.discover.ui.catalog

import com.luum.michi.app.core.media.domain.MediaGridItem
import com.luum.michi.app.discover.domain.model.DashboardCardStat
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.cardStats
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.ui.components.popularityBadge
import com.luum.michi.app.ui.components.scoreBadge

/**
 * Catalog cell with the rail's own badges ([DashboardSectionKind.cardStats]):
 * the "Most popular" shelves lead with popularity, the rest with the score, so
 * the "View all" grid matches the rail it came from. Mapping lives in `discover`
 * because `ui/components` cannot import a feature model.
 */
internal fun MediaGridItem.toCatalogPosterItem(kind: DashboardSectionKind): MediaRailItem = MediaRailItem(
    id = mediaId,
    title = title,
    image = cover,
    subtitle = genres.take(2).joinToString(" · ").ifBlank { null },
    placeholderHex = paletteHex,
    stats = kind.cardStats().mapNotNull { stat ->
        when (stat) {
            DashboardCardStat.SCORE -> scoreBadge(averageScore)
            DashboardCardStat.POPULARITY -> popularityBadge(popularity)
        }
    },
)
