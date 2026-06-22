package com.luum.michi.app.account.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.platform.components.PlatformHomeSection

/** Rail preview cap; the rest are reached via the "See all" affordance. */
private const val RailPreviewLimit = 10

/**
 * Favorites section sharing Discover's [PlatformHomeSection] header ("See all").
 *
 * Always renders a horizontal rail ([LazyRow]) showing the first
 * [RailPreviewLimit] items, like Discover's rails.
 */
@Composable
internal fun <T : Any> AccountFavoriteSection(
    title: String,
    items: List<T>,
    onSeeAll: () -> Unit,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) return

    PlatformHomeSection(title = title, onSeeAll = onSeeAll) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = items.take(RailPreviewLimit), key = itemKey) { item ->
                itemContent(item)
            }
        }
    }
}
