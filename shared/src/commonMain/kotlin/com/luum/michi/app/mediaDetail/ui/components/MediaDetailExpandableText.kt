package com.luum.michi.app.mediaDetail.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.mediaDetail.domain.common.BiographyStat
import com.luum.michi.app.mediaDetail.domain.common.InlineSpoiler
import com.luum.michi.app.mediaDetail.domain.common.SpoilerSegment
import com.luum.michi.app.mediaDetail.domain.common.SpoilerText
import com.luum.michi.app.mediaDetail.domain.common.toSegments
import com.luum.michi.app.ui.components.SectionHeader

/**
 * Collapsible long-text section shared by every detail scope (media
 * synopsis, character/staff biography). Painted inside [MediaDetailPanel]: the
 * prose is a data block, like the information grid and the link rows. Collapsed
 * to five lines; the read more/less link only appears when the collapsed text
 * actually overflows (or while expanded, so it can always be folded back).
 *
 * [stats] are the biography's `Label: value` lines AniList marked in bold (read
 * by `core/util`): they become a stat block above the prose, with the label in
 * bold and the value in `onSurface`. A synopsis passes none.
 *
 * [spoilers] are hidden ranges of [text]. Each one is painted as [strings]'
 * spoiler label and becomes a link: tapping it reveals only that range, through
 * [spoilerState]. The body text is deliberately not clickable and the container
 * is not either — expansion stays on the read more/less row, so revealing a
 * spoiler can never also expand or collapse the block.
 */
@Composable
internal fun MediaDetailExpandableText(
    title: String,
    text: String,
    strings: LanguageStrings,
    spoilerState: SpoilerRevealState,
    modifier: Modifier = Modifier,
    // Person biographies carry the bold stats; a synopsis does not.
    stats: List<BiographyStat> = emptyList(),
    spoilers: List<IntRange> = emptyList(),
) {
    var expanded by remember(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }
    val hiddenStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        background = MaterialTheme.colorScheme.surfaceVariant,
    )
    val segments = remember(text, spoilers) { SpoilerText(text, spoilers).toSegments() }
    val visibleSegments = if (expanded) segments else segments.collapsedSegments()
    val hasMoreSegments = visibleSegments.size < segments.size
    MediaDetailPanel(modifier = modifier) {
        SectionHeader(title)
        Spacer(modifier = Modifier.height(8.dp))
        if (stats.isNotEmpty()) {
            BiographyStats(stats)
        }
        if (segments.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                visibleSegments.forEach { segment ->
                    when (segment) {
                        is SpoilerSegment.Prose -> Text(
                            text = segment.text.withHiddenSpoilers(
                                inlineSpoilers = segment.inlineSpoilers,
                                spoilerState = spoilerState,
                                hiddenLabel = strings.mediaDetailSpoilerHidden,
                                hiddenStyle = hiddenStyle,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
                            overflow = TextOverflow.Ellipsis,
                            // Measured only while collapsed: once expanded there
                            // is no overflow to report, and the link stays via
                            // [expanded].
                            onTextLayout = { result ->
                                if (!expanded) overflows = result.hasVisualOverflow
                            },
                        )
                        is SpoilerSegment.Block -> SpoilerBlock(
                            text = segment.text,
                            isRevealed = spoilerState.isRevealed(segment.spoilerIndex),
                            onToggle = { spoilerState.toggle(segment.spoilerIndex) },
                            strings = strings,
                            bordered = false,
                        )
                    }
                }
            }
        }
        if (expanded || overflows || hasMoreSegments) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (expanded) strings.mediaDetailReadLessAction else strings.mediaDetailReadMoreAction,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
            )
        }
    }
}

/**
 * Replaces every hidden range with the spoiler label as a clickable link. A
 * revealed range keeps its real text, and only its own index is revealed.
 */
private fun String.withHiddenSpoilers(
    inlineSpoilers: List<InlineSpoiler>,
    spoilerState: SpoilerRevealState,
    hiddenLabel: String,
    hiddenStyle: SpanStyle,
): AnnotatedString {
    val source = this
    return buildAnnotatedString {
        var cursor = 0
        inlineSpoilers.forEach { inline ->
            val start = inline.range.first.coerceIn(cursor, source.length)
            val end = (inline.range.last + 1).coerceIn(start, source.length)
            if (start > cursor) append(source.substring(cursor, start))
            if (spoilerState.isRevealed(inline.spoilerIndex)) {
                append(source.substring(start, end))
            } else {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "spoiler:${inline.spoilerIndex}",
                        styles = TextLinkStyles(style = hiddenStyle),
                        linkInteractionListener = LinkInteractionListener {
                            spoilerState.reveal(inline.spoilerIndex)
                        },
                    ),
                ) {
                    append(hiddenLabel)
                }
            }
            cursor = end
        }
        if (cursor < source.length) append(source.substring(cursor))
    }
}

/**
 * Collapsed view: everything through the first prose paragraph. Blocks before
 * it stay visible (closed and toggleable); prose and blocks after it need "read
 * more". A text made only of blocks shows all of them.
 */
private fun List<SpoilerSegment>.collapsedSegments(): List<SpoilerSegment> {
    val firstProse = indexOfFirst { it is SpoilerSegment.Prose }
    return if (firstProse == -1) this else take(firstProse + 1)
}

/** The stat block: the label in bold, the value in `onSurface` (wrapping). */
@Composable
private fun BiographyStats(stats: List<BiographyStat>) {
    val valueColor = MaterialTheme.colorScheme.onSurface
    Column(modifier = Modifier.fillMaxWidth()) {
        stats.forEach { stat ->
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stat.label) }
                    append(": ")
                    withStyle(SpanStyle(color = valueColor)) { append(stat.value) }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // No maxLines on purpose: a long value wraps instead of clipping.
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private const val COLLAPSED_LINES = 5
