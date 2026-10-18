package com.luum.michi.app.discover.ui.catalog

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.media.domain.label
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.defaultSeason
import com.luum.michi.app.discover.domain.model.selectedSeason
import com.luum.michi.app.discover.domain.model.seasonRail
import com.luum.michi.app.discover.ui.catalog.state.CatalogGridStateHolder
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.language.Strings

/**
 * "View all" of one Dashboard rail: its catalog filter as a poster grid, with
 * the same server order and pagination as the genre page. The rail's title and
 * the sort live in the toolbar (the bar carries the title, like a genre); the
 * catalog has no Anime/Manga switch — its type is fixed by the filter.
 *
 * A season rail (SEASONAL / UPCOMING_SEASON) adds a 4-season chip row as the
 * grid's header (the same scrolling-header mechanism the genre page uses for
 * its type switch): tapping one re-binds the holder to the same filter with
 * that season, and the toolbar title follows the selected chip.
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
    // The rail window is anchored on the kind's default season once per page
    // instance, so picking another chip never shifts it.
    val seasons = remember(kind) { kind.seasonRail() }
    val selectedSeason = holder.filter.selectedSeason()
        ?: seasons?.defaultSeason(kind)
    val seasonHeader: (LazyGridScope.() -> Unit)? = seasons?.let { rail ->
        {
            item(key = "season", span = { GridItemSpan(maxLineSpan) }) {
                ChipRail(
                    tabs = rail.map { ChipTab(value = it, label = it.label(strings)) },
                    selected = selectedSeason,
                    onSelect = { holder.changeSeason(it) },
                )
            }
        }
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
        header = seasonHeader,
    )
}
