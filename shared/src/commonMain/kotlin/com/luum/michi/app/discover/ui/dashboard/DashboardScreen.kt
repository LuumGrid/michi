package com.luum.michi.app.discover.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.discover.domain.model.MediaItem
import com.luum.michi.app.discover.ui.dashboard.state.DashboardStateHolder
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.MediaRail
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.PullRefresh
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/** One dashboard rail: localized title + its media. */
private data class DashboardSection(val title: String, val items: List<MediaItem>)

/**
 * DISCOVER dashboard: an Anime/Manga selector over the holder's rails.
 * Public catalog data, so guests render the same surface. Loads are explicit
 * (TTL dedups revisits); an 18+ setting change forces a reload.
 */
@Composable
internal fun DashboardScreen(
    holder: DashboardStateHolder,
    mediaType: DashboardMediaType,
    onSelectMediaType: (DashboardMediaType) -> Unit,
    onOpenMedia: (Int) -> Unit,
    hideAdult: Boolean,
    strings: LanguageStrings = Strings.current,
    modifier: Modifier = Modifier,
    // Scaffold bottom threaded from Root: the last rail clears the floating
    // TabBar. Never a per-feature constant.
    bottomPadding: Dp = 0.dp,
) {
    LaunchedEffect(hideAdult) {
        holder.setHideAdult(hideAdult)
        holder.load()
    }
    DashboardContent(
        holder = holder,
        mediaType = mediaType,
        onSelectMediaType = onSelectMediaType,
        onOpenMedia = onOpenMedia,
        strings = strings,
        modifier = modifier,
        bottomPadding = bottomPadding,
        onRefresh = { holder.load(forceRefresh = true) },
        onRetry = { holder.load(forceRefresh = true) },
    )
}

@Composable
private fun DashboardContent(
    holder: DashboardStateHolder,
    mediaType: DashboardMediaType,
    onSelectMediaType: (DashboardMediaType) -> Unit,
    onOpenMedia: (Int) -> Unit,
    strings: LanguageStrings,
    bottomPadding: Dp,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = dashboardSections(holder, mediaType, strings)
    Column(modifier = modifier.fillMaxSize()) {
        ChipRail(
            tabs = listOf(
                ChipTab(DashboardMediaType.ANIME, strings.animeLabel),
                ChipTab(DashboardMediaType.MANGA, strings.mangaLabel),
            ),
            selected = mediaType,
            onSelect = onSelectMediaType,
        )
        when {
            holder.isLoading && sections.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.discoverLoadingLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            holder.error != null && sections.isEmpty() -> {
                MessagePanel(
                    title = strings.discoverErrorLabel,
                    message = holder.error?.let { strings.networkErrorMessage(it) },
                    icon = Icons.Filled.Warning,
                    actionLabel = strings.retryAction,
                    onAction = onRetry,
                )
            }
            sections.isEmpty() -> {
                MessagePanel(
                    title = strings.discoverEmptyLabel,
                    message = null,
                    icon = AppIcons.Search,
                    actionLabel = null,
                    onAction = {},
                )
            }
            else -> {
                PullRefresh(
                    isRefreshing = holder.isRefreshing,
                    onRefresh = onRefresh,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 16.dp + bottomPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        items(
                            items = sections,
                            key = { it.title },
                        ) { section ->
                            // Rails stay edge-to-edge (card vs edge-to-edge rule):
                            // a rail boxed in a card clips on the rounded corners.
                            MediaRail(
                                title = section.title,
                                items = section.items.mapNotNull { it.toRailItem() },
                                onSelect = onOpenMedia,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Visible rails for [mediaType], dropping any that came back empty. */
private fun dashboardSections(
    holder: DashboardStateHolder,
    mediaType: DashboardMediaType,
    strings: LanguageStrings,
): List<DashboardSection> = when (mediaType) {
    DashboardMediaType.ANIME -> listOf(
        DashboardSection(strings.discoverTrendingAnimeTitle, holder.trendingAnime),
        DashboardSection(strings.exploreThisSeasonTitle, holder.thisSeason),
        DashboardSection(strings.exploreUpcomingNextSeasonTitle, holder.upcomingNextSeason),
        DashboardSection(strings.exploreAllTimePopularAnimeTitle, holder.allTimePopularAnime),
        DashboardSection(strings.exploreTopAnimeTitle, holder.topAnime),
    )
    DashboardMediaType.MANGA -> listOf(
        DashboardSection(strings.discoverTrendingMangaTitle, holder.trendingManga),
        DashboardSection(strings.exploreAllTimePopularMangaTitle, holder.allTimePopularManga),
        DashboardSection(strings.exploreTopMangaTitle, holder.topManga),
    )
}.filter { it.items.isNotEmpty() }

/** Catalog items with a missing id cannot navigate, so they never reach a rail. */
private fun MediaItem.toRailItem(): MediaRailItem? = id?.let {
    MediaRailItem(
        id = it,
        title = title,
        imageUrl = coverUrl,
        subtitle = meta,
    )
}
