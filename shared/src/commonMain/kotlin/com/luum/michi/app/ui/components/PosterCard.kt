package com.luum.michi.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.model.CoverSizes

/**
 * The one poster card of the app: a portrait cover (token-sized) + up to two
 * cover badges + two reserved title lines + an optional subtitle. Rails, the
 * "View all" grids and the studio grid all draw this card, so a poster looks the
 * same everywhere (one radius; the type scale steps with the token: a [CoverSize.Rail]
 * card reads `bodySmall`/`labelSmall`, a [CoverSize.Featured] one `bodyMedium`/
 * `bodySmall`). Takes only primitives, so no feature model crosses into `ui`. A
 * null [onClick] renders a display-only card. [reserveTitleLines] keeps two title
 * lines even for a one-line title (rails stay even); grids pass false so the
 * subtitle hugs the title.
 */
@Composable
internal fun PosterCard(
    cover: CoverSizes?,
    title: String,
    size: CoverSize,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    subtitle: String? = null,
    placeholderHex: String? = null,
    stats: List<CoverStat> = emptyList(),
    reserveTitleLines: Boolean = true,
    // Extra cover overlay painted after the badges (e.g. an edit-mode remove
    // button); a null keeps every existing card byte-identical.
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    // A larger token gets one step up in the type scale, so a featured card
    // reads as a bigger card instead of a rail card with more air.
    val isLarge = size == CoverSize.Featured
    // Capped font scale: the badges and the title must not collide on a dense
    // card (same rule as MediaCoverCard).
    CardCappedDensity {
        Column(
            modifier = modifier
                .width(size.width)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        ) {
            CoverImage(
                cover = cover,
                size = size,
                placeholderHex = placeholderHex,
                overlay = {
                    // First badge top-start, second top-end (max two by design).
                    stats.getOrNull(0)?.let {
                        CoverStatBadge(
                            stat = it,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp),
                        )
                    }
                    stats.getOrNull(1)?.let {
                        CoverStatBadge(
                            stat = it,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                        )
                    }
                    overlay?.invoke(this)
                },
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = if (isLarge) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                // Rails reserve both lines (the row stays even); a grid lets the
                // subtitle hug the title.
                minLines = if (reserveTitleLines) 2 else 1,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = if (isLarge) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
