package com.luum.michi.app.discover.ui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.ui.catalog.state.CatalogGridStateHolder
import com.luum.michi.app.ui.language.Strings

/**
 * "View all" of one Dashboard rail: its catalog filter as a poster grid, with
 * the same server order and pagination as the genre page. The rail's title and
 * the sort live in the toolbar (the bar carries the title, like a genre); the
 * catalog has no Anime/Manga switch — its type is fixed by the filter.
 */
@Composable
internal fun CatalogDetailScreen(
    holder: CatalogGridStateHolder,
    kind: DashboardSectionKind,
    strings: LanguageStrings = Strings.current,
    bottomPadding: Dp = 0.dp,
    onRetry: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember(holder.media.items, kind) {
        holder.media.items.map { it.toCatalogPosterItem(kind) }
    }
    CatalogGridContent(
        holder = holder,
        items = items,
        strings = strings,
        bottomPadding = bottomPadding,
        // Scroll rule: a filter/sort change returns the grid to the top.
        resetKey = listOf(holder.filter, holder.sortOption, holder.sortOrder),
        onRetry = onRetry,
        onOpenMedia = onOpenMedia,
        modifier = modifier,
    )
}
