package com.luum.michi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * Shared cover row: portrait cover + title + optional subtitle + caller-owned
 * extras slot. Takes only primitives, so any feature showing media (lists,
 * discover, favorites, calendar, detail relations) reuses it. Feature-specific
 * content (progress counters, +1 buttons, ratings) goes in the [content] slot
 * and lives in the calling feature — never here. Same for the optional
 * [titleTrailing] action (e.g. the list edit pencil): caller-owned, hidden
 * by default.
 *
 * Content surface, not glass: cards scroll with the list (see the glass
 * rule in CONTEXT).
 *
 * Uniform height: every card measures the same, so covers never stretch
 * differently. The fixed height fits the known content budget (title 2
 * lines + subtitle + meta line + bottom rail); overflow discipline (maxLines)
 * on the text side keeps longer content from clipping. OS font scale is
 * capped inside the card (see CardCappedDensity).
 */
@Composable
internal fun MediaCoverCard(
    coverUrl: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    CardCappedDensity {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
            color = MaterialTheme.colorScheme.surface,
        ) {
            // Fixed height: bounded, so fillMaxHeight measures plain (no
            // intrinsics — the SubcomposeLayout attempt crashed on Coil).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CardHeight)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaCoverImage(
                    coverUrl = coverUrl,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(3f / 4f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Header top-anchored, slot filling the rest: bottom pills
                // terminate flush with the cover bottom. Bounded by the fixed Row
                // height, so weight distributes (in wrap content it would collapse
                // and float the pills, as before).
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Top,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (titleTrailing != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            titleTrailing()
                        }
                    }
                    content?.invoke(this)
                }
            }
        }
    }
}

/**
 * Caps the OS font scale inside dense fixed-height cards, so large-text
 * settings degrade to ellipsis instead of clipping card content.
 * Full-scale access lives on the detail screens (unbounded layouts).
 */
@Composable
private fun CardCappedDensity(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val capped = remember(density) {
        Density(density.density, density.fontScale.coerceAtMost(CardFontScaleCap))
    }
    CompositionLocalProvider(LocalDensity provides capped, content = content)
}

private const val CardFontScaleCap = 1.2f

private val CardHeight = 172.dp

@Composable
private fun MediaCoverImage(
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    if (coverUrl.isNullOrBlank()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        SubcomposeAsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            },
            error = {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            },
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
        )
    }
}
