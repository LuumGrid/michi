package com.luum.michi.app.discover.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.media.domain.currentSeasonAndYear
import com.luum.michi.app.core.media.domain.next
import com.luum.michi.app.discover.domain.model.DashboardCardStat
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.MediaItem
import com.luum.michi.app.discover.domain.model.cardStats
import com.luum.michi.app.discover.domain.model.title
import com.luum.michi.app.discover.ui.dashboard.state.DashboardStateHolder
import com.luum.michi.app.ui.components.CatalogChipRail
import com.luum.michi.app.ui.components.CoverStat
import com.luum.michi.app.ui.components.LoadStateContainer
import com.luum.michi.app.ui.components.MediaCardRail
import com.luum.michi.app.ui.components.MediaCardRailRows
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.popularityBadge
import com.luum.michi.app.ui.components.scoreBadge
import com.luum.michi.app.ui.components.rememberResetListState
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.TestTags
import com.luum.michi.app.ui.language.Strings

/**
 * DISCOVER dashboard: an Anime/Manga selector over the holder's rails. Only
 * the selected type is fetched (one query per type), so the payload stays the
 * same as before even with ~10 rails each. Public, so guests see it.
 */
@Composable
internal fun DashboardScreen(
    holder: DashboardStateHolder,
    onOpenMedia: (Int) -> Unit,
    onOpenCatalog: (String) -> Unit,
    hideAdult: Boolean,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
    // Scaffold bottom threaded from Root: the last rail clears the floating
    // TabBar. Never a per-feature constant.
    bottomPadding: Dp = 0.dp,
) {
    // `generation` is a key so a language change (invalidate()) reloads when the
    // dashboard is visible; on another tab this effect only runs on (re)entry.
    LaunchedEffect(holder, hideAdult, holder.generation) {
        holder.setHideAdult(hideAdult)
        holder.load()
    }
    DashboardContent(
        holder = holder,
        onOpenMedia = onOpenMedia,
        onOpenCatalog = onOpenCatalog,
        strings = strings,
        modifier = modifier,
        bottomPadding = bottomPadding,
        onRetry = { holder.load(forceRefresh = true) },
    )
}

@Composable
private fun DashboardContent(
    holder: DashboardStateHolder,
    onOpenMedia: (Int) -> Unit,
    onOpenCatalog: (String) -> Unit,
    strings: LanguageStrings,
    bottomPadding: Dp,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = currentSeasonAndYear()
    val next = current.next()
    // Scroll rule: switching the Anime/Manga selector returns to the top.
    val listState = rememberResetListState(holder.mediaType)
    val sections = DashboardSectionKind.entries
        .filter { it.mediaType == holder.mediaType }
        .mapNotNull { kind ->
            holder.sections[kind]
                ?.takeIf { it.isNotEmpty() }
                ?.let { kind to it }
        }
    val errorMessage = holder.error?.let { strings.networkErrorMessage(it) }
    // Once the visible type has content, warm the other type in the background
    // (guarded inside; respects the cache, never surfaces an error).
    LaunchedEffect(sections.isNotEmpty()) {
        if (sections.isNotEmpty()) holder.preloadOther()
    }
    Column(modifier = modifier.fillMaxSize()) {
        CatalogChipRail(
            anime = DashboardMediaType.ANIME,
            manga = DashboardMediaType.MANGA,
            selected = holder.mediaType,
            onSelect = holder::selectType,
            strings = strings,
        )
        LoadStateContainer(
            hasContent = sections.isNotEmpty(),
            isLoading = holder.isLoading || holder.isRefreshing,
            errorMessage = errorMessage,
            loadingLabel = strings.discoverLoadingLabel,
            waitingLabel = strings.networkThrottleWaitingLabel,
            errorTitle = strings.discoverErrorLabel,
            retryLabel = strings.retryAction,
            onRetry = onRetry,
            // With content already on screen, a failed refresh is a compact
            // line above the rails, not a full-page error.
            refreshError = if (sections.isNotEmpty()) errorMessage else null,
            modifier = Modifier.fillMaxSize(),
            // Loaded, but every rail came back empty.
            empty = {
                MessagePanel(
                    title = strings.discoverEmptyLabel,
                    message = null,
                    icon = AppIcons.Search,
                )
            },
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.testTag(TestTags.Dashboard),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = 16.dp + bottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(
                    items = sections,
                    key = { it.first },
                ) { (kind, media) ->
                    // Rails stay edge-to-edge (card vs edge-to-edge rule):
                    // a rail boxed in a card clips on the rounded corners.
                    MediaCardRail(
                        title = kind.title(strings, current, next),
                        items = media.mapNotNull { it.toRailItem(kind) },
                        // The leading rail of each selector is one big card;
                        // the "best" shelves are ranked columns of three.
                        rows = if (kind.isFeatured) {
                            MediaCardRailRows.ONE
                        } else {
                            MediaCardRailRows.THREE
                        },
                        numbered = kind.isRanked,
                        onSelect = onOpenMedia,
                        // "View all" opens the same catalog as a full page.
                        onViewAll = { onOpenCatalog(kind.name) },
                        viewAllLabel = strings.viewAllAction,
                    )
                }
            }
        }
    }
}

/** The leading rail of each selector (one row instead of three). */
private val DashboardSectionKind.isFeatured: Boolean
    get() = this == DashboardSectionKind.SEASONAL || this == DashboardSectionKind.NEW_RELEASES_MANGA

/** Catalog items with a missing id cannot navigate, so they never reach a rail. */
private fun MediaItem.toRailItem(kind: DashboardSectionKind): MediaRailItem? = id?.let {
    MediaRailItem(
        id = it,
        title = title,
        image = cover,
        subtitle = meta,
        placeholderHex = placeholderHex,
        stats = statChips(kind),
    )
}

/** The rail's chips for one item, in paint order; a null value paints nothing. */
private fun MediaItem.statChips(kind: DashboardSectionKind): List<CoverStat> =
    kind.cardStats().mapNotNull { stat ->
        when (stat) {
            DashboardCardStat.SCORE -> scoreBadge(averageScore)
            DashboardCardStat.POPULARITY -> popularityBadge(popularity)
        }
    }
