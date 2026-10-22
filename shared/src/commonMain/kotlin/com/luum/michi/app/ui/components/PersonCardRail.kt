package com.luum.michi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
 * [primary] with [secondary] in the card's actor row in the double one.
 */
internal data class PersonRailItem(
    val primary: PersonRailPerson,
    val secondary: PersonRailPerson? = null,
)

private val PersonCardRailGap = 12.dp
private val PersonCardRailRowGap = 8.dp
private const val PersonCardRailRows = 3

/**
 * Horizontal rail of people cards, three per column, snapped like
 * [MediaCardRail] and sharing its header, empty label and scroll rule.
 *
 * Every card is the shared [MediaCoverCard] at [CoverSize.Compact]. When any
 * item carries a [PersonRailItem.secondary] the whole rail is in double mode:
 * the character is the card itself (one title line + role) and the actor takes
 * the bottom row — the slot where [MediaCardRail] paints its chips — with its
 * own tap target. Otherwise the card is the simple title + role layout.
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
                                MediaCoverCard(
                                    cover = item.primary.image,
                                    title = item.primary.name,
                                    subtitle = item.primary.subtitle,
                                    onClick = { onSelectPrimary(item.primary.id) },
                                    modifier = Modifier.testTag(TestTags.Poster),
                                    titleMaxLines = if (double) 1 else 2,
                                    subtitleMaxLines = 1,
                                    coverSize = CoverSize.Compact,
                                    content = item.secondary?.let { secondary ->
                                        {
                                            Spacer(modifier = Modifier.weight(1f))
                                            PersonCardRailActorRow(
                                                person = secondary,
                                                onSelect = onSelectSecondary,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The actor row of a double card: avatar, one-line name and optional language,
 * on the flat pill that owns its own tap target. The trailing 12dp keeps the
 * text clear of the rounded end.
 */
@Composable
private fun PersonCardRailActorRow(
    person: PersonRailPerson,
    onSelect: ((Int) -> Unit)?,
) {
    val interaction = onSelect?.let { select ->
        Modifier.clickable(role = Role.Button) { select(person.id) }
    } ?: Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AvatarSize.Row)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(interaction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            imageUrl = person.image?.medium ?: person.image?.large,
            initials = person.name.trim().take(1).uppercase().ifEmpty { "?" },
            size = AvatarSize.Row,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            person.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
    }
}
