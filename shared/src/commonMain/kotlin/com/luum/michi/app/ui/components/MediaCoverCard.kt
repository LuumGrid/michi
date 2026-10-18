package com.luum.michi.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.model.CoverSizes

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
 *
 * The cover is drawn from [CoverSize.List] (width **and** height) and the row
 * height derives from it, so the rendered poster always matches the token.
 */
@Composable
internal fun MediaCoverCard(
    cover: CoverSizes?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    // Line budget of the two text slots. The card's height is fixed, so a longer
    // budget only spends the air the row already reserves (still ellipsized).
    titleMaxLines: Int = 2,
    subtitleMaxLines: Int = 1,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    CardCappedDensity {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = PanelShape,
            border = flatBorder(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            // Fixed row height: bounded, so the text column distributes with
            // weight and the cover keeps the standard 3:4 (no intrinsics — the
            // SubcomposeLayout attempt crashed on Coil).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CardHeight)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Both dimensions come from the token; the row height derives
                // from it, so the cover and the card can never drift apart.
                CoverImage(cover = cover, size = CoverSize.List)
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
                                maxLines = titleMaxLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = subtitleMaxLines,
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

/** List-row height so its cover is exactly [CoverSize.List] plus the row padding. */
private val CardHeight = CoverSize.List.height + 24.dp
