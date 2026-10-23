package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.ui.TestTags

/** Inner air of a poster card: the cover and the rounded surface share it. */
private val PosterCardPadding = 8.dp

/**
 * The one poster card of the app: a flat bordered card ([PanelShape] +
 * [flatBorder] + `surface`, the same recipe as [MediaCoverCard]) with the cover
 * inset by [PosterCardPadding], a two-line title and an optional subtitle under
 * it. The caller can reserve the subtitle ([reserveSubtitle]) so every cell of
 * a grid measures the same even when some items lack it. No badges over the
 * cover; the type scale steps with the token: a [CoverSize.Rail] card reads
 * `bodySmall`/`labelSmall`, a [CoverSize.Featured] one or wider
 * `bodyMedium`/`bodySmall`. Takes only primitives, so no feature model crosses
 * into `ui`. A null [onClick] renders a display-only card.
 */
@Composable
internal fun PosterCard(
    cover: CoverSizes?,
    title: String,
    size: CoverSize,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    subtitle: String? = null,
    reserveSubtitle: Boolean = false,
    placeholderHex: String? = null,
    // Extra cover overlay painted last (e.g. an edit-mode remove button).
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    // A larger token gets one step up in the type scale, so a featured card
    // reads as a bigger card instead of a rail card with more air. Width-based
    // (not ==): a grid cell grows up to +25%, so the token drifts past
    // Featured's exact width.
    val isLarge = size.width >= CoverSize.Featured.width
    // Capped font scale: the title and subtitle degrade to ellipsis instead of
    // clipping (same rule as MediaCoverCard).
    val base = modifier
        .width(size.width)
        .testTag(TestTags.Poster)
    CardCappedDensity {
        if (onClick != null) {
            Surface(
                onClick = onClick,
                shape = PanelShape,
                border = flatBorder(),
                color = MaterialTheme.colorScheme.surface,
                modifier = base,
            ) {
                PosterCardContent(
                    cover = cover,
                    title = title,
                    size = size,
                    isLarge = isLarge,
                    subtitle = subtitle,
                    reserveSubtitle = reserveSubtitle,
                    placeholderHex = placeholderHex,
                    overlay = overlay,
                )
            }
        } else {
            Surface(
                shape = PanelShape,
                border = flatBorder(),
                color = MaterialTheme.colorScheme.surface,
                modifier = base,
            ) {
                PosterCardContent(
                    cover = cover,
                    title = title,
                    size = size,
                    isLarge = isLarge,
                    subtitle = subtitle,
                    reserveSubtitle = reserveSubtitle,
                    placeholderHex = placeholderHex,
                    overlay = overlay,
                )
            }
        }
    }
}

/** The card's interior: inset cover + two-line title + optional subtitle line. */
@Composable
private fun PosterCardContent(
    cover: CoverSizes?,
    title: String,
    size: CoverSize,
    isLarge: Boolean,
    subtitle: String?,
    reserveSubtitle: Boolean,
    placeholderHex: String?,
    overlay: (@Composable BoxScope.() -> Unit)?,
) {
    Column(modifier = Modifier.padding(PosterCardPadding)) {
        CoverImage(
            cover = cover,
            size = CoverSize(size.width - PosterCardPadding * 2),
            placeholderHex = placeholderHex,
            overlay = {
                overlay?.invoke(this)
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = if (isLarge) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        when {
            subtitle != null -> Text(
                text = subtitle,
                style = if (isLarge) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            reserveSubtitle -> Text(
                text = "",
                style = if (isLarge) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 1,
                maxLines = 1,
            )
        }
    }
}
