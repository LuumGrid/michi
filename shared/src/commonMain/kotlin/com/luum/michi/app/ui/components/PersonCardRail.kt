package com.luum.michi.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.ui.TestTags

/** One person of a [PersonCardRail]: portrait + name + optional subtitle. */
internal data class PersonRailPerson(
    val id: Int,
    val name: String,
    val image: CoverSizes?,
    val subtitle: String? = null,
)

/**
 * One card of a [PersonCardRail]: [primary] alone in the simple layout, or
 * [primary] + [secondary] side by side in the double one (character + actor).
 */
internal data class PersonRailItem(
    val primary: PersonRailPerson,
    val secondary: PersonRailPerson? = null,
)

private val PersonCardRailGap = 12.dp
private val PersonCardRailRowGap = 8.dp
private val PersonCardRailPairGap = 8.dp
private const val PersonCardRailRows = 3

/**
 * Horizontal rail of people cards, three per column, snapped like
 * [MediaCardRail] and sharing its header, empty label and scroll rule.
 *
 * A rail where any item carries a [PersonRailItem.secondary] switches the whole
 * rail to the double card (character + Japanese voice actor): one column of
 * three pairs, each half tap-able on its own. Otherwise every card is the
 * standard [MediaCoverCard] over [CoverSize.Compact]. Card sizes derive from the
 * compact token only — no per-rail measurements.
 */
@Composable
internal fun PersonCardRail(
    title: String?,
    items: List<PersonRailItem>,
    onSelectPrimary: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onSelectSecondary: ((Int) -> Unit)? = null,
    onViewAll: (() -> Unit)? = null,
    viewAllLabel: String? = null,
    emptyLabel: String? = null,
    resetKey: Any? = null,
) {
    val rowState = rememberResetListState(resetKey)
    val double = items.any { it.secondary != null }
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            RailHeader(
                title = title,
                viewAllLabel = viewAllLabel,
                onViewAll = onViewAll,
            )
        }
        if (items.isEmpty() && emptyLabel != null) {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = RailHorizontalPadding, vertical = 8.dp),
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // One column per viewport minus the peek: the next column always
                // shows a sliver, and snapping lands on a column boundary.
                val columnWidth = (
                    maxWidth - RailHorizontalPadding * 2 - PersonCardRailGap - MediaCardRailPeek
                ).coerceAtLeast(0.dp)
                // Fixed column height, even for the last column with one or two
                // cards: the rail never changes height while scrolling.
                val cardHeight = mediaCoverCardHeight(CoverSize.Compact)
                val columnHeight = (cardHeight + PersonCardRailRowGap) * PersonCardRailRows -
                    PersonCardRailRowGap
                val columns = items.chunked(PersonCardRailRows)
                LazyRow(
                    state = rowState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.Rail),
                    contentPadding = PaddingValues(horizontal = RailHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(PersonCardRailGap),
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = rowState),
                ) {
                    itemsIndexed(
                        items = columns,
                        key = { index, _ -> "column#$index" },
                    ) { _, column ->
                        Column(
                            modifier = Modifier
                                .width(columnWidth)
                                .height(columnHeight),
                            verticalArrangement = Arrangement.spacedBy(PersonCardRailRowGap),
                        ) {
                            column.forEach { item ->
                                if (double) {
                                    PersonDoubleCard(
                                        item = item,
                                        onSelectPrimary = onSelectPrimary,
                                        onSelectSecondary = onSelectSecondary,
                                    )
                                } else {
                                    val primary = item.primary
                                    MediaCoverCard(
                                        cover = primary.image,
                                        title = primary.name,
                                        subtitle = primary.subtitle,
                                        onClick = { onSelectPrimary(primary.id) },
                                        modifier = Modifier.testTag(TestTags.Poster),
                                        titleMaxLines = 2,
                                        subtitleMaxLines = 1,
                                        coverSize = CoverSize.Compact,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Character + voice actor in one fixed-height card. The [Surface] owns the
 * frame only; each half carries its own tap target, and a missing [secondary]
 * leaves the right half inert.
 */
@Composable
private fun PersonDoubleCard(
    item: PersonRailItem,
    onSelectPrimary: (Int) -> Unit,
    onSelectSecondary: ((Int) -> Unit)?,
) {
    CardCappedDensity {
        Surface(
            shape = PanelShape,
            border = flatBorder(),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(mediaCoverCardHeight(CoverSize.Compact))
                .testTag(TestTags.Poster),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MediaCoverCardPadding),
            ) {
                PersonHalf(
                    person = item.primary,
                    alignEnd = false,
                    onClick = { onSelectPrimary(item.primary.id) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                Spacer(modifier = Modifier.width(PersonCardRailPairGap))
                val secondary = item.secondary
                if (secondary != null) {
                    val secondaryClick = onSelectSecondary?.let { select ->
                        { select(secondary.id) }
                    }
                    PersonHalf(
                        person = secondary,
                        alignEnd = true,
                        onClick = secondaryClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                } else {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/**
 * One half of [PersonDoubleCard]: portrait + (up to) three lines, mirrored on
 * the right so the two portraits sit on the card's outer edges.
 */
@Composable
private fun PersonHalf(
    person: PersonRailPerson,
    alignEnd: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val interaction = if (onClick != null) {
        Modifier
            .clip(CoverShape)
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = modifier.then(interaction),
        verticalAlignment = Alignment.Top,
    ) {
        if (!alignEnd) {
            CoverImage(cover = person.image, size = CoverSize.Compact)
            Spacer(modifier = Modifier.width(PersonCardRailPairGap))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            )
            person.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
                )
            }
        }
        if (alignEnd) {
            Spacer(modifier = Modifier.width(PersonCardRailPairGap))
            CoverImage(cover = person.image, size = CoverSize.Compact)
        }
    }
}
