package com.luum.michi.app.discover.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.model.MediaSeasonYear
import com.luum.michi.app.core.model.currentSeasonAndYear
import com.luum.michi.app.core.model.label
import com.luum.michi.app.core.model.next
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.MediaItem
import com.luum.michi.app.discover.ui.dashboard.state.DashboardStateHolder
import com.luum.michi.app.ui.components.ChipRail
import com.luum.michi.app.ui.components.ChipTab
import com.luum.michi.app.ui.components.MediaCoverCardCoverWidth
import com.luum.michi.app.ui.components.MediaRail
import com.luum.michi.app.ui.components.MediaRailItem
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.PullRefresh
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings

/** Featured rails (current season / new releases) lead, 50% bigger covers. */
private const val FeaturedCoverScale = 1.5f

/**
 * DISCOVER dashboard: an Anime/Manga selector over the holder's rails. Only
 * the selected type is fetched (one query per type), so the payload stays the
 * same as before even with ~10 rails each. Public, so guests see it.
 */
@Composable
internal fun DashboardScreen(
    holder: DashboardStateHolder,
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
    onOpenMedia: (Int) -> Unit,
    strings: LanguageStrings,
    bottomPadding: Dp,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = currentSeasonAndYear()
    val next = current.next()
    val sections = DashboardSectionKind.entries
        .filter { it.mediaType == holder.mediaType }
        .mapNotNull { kind ->
            holder.sections[kind]
                ?.takeIf { it.isNotEmpty() }
                ?.let { kind to it }
        }
    Column(modifier = modifier.fillMaxSize()) {
        ChipRail(
            tabs = listOf(
                ChipTab(DashboardMediaType.ANIME, strings.animeLabel),
                ChipTab(DashboardMediaType.MANGA, strings.mangaLabel),
            ),
            selected = holder.mediaType,
            onSelect = holder::selectType,
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
                        if (holder.error != null) {
                            item(key = "refresh_error") {
                                DashboardRefreshError(
                                    message = strings.networkErrorMessage(holder.error!!),
                                    retryLabel = strings.retryAction,
                                    onRetry = onRetry,
                                )
                            }
                        }
                        items(
                            items = sections,
                            key = { it.first },
                        ) { (kind, media) ->
                            // Rails stay edge-to-edge (card vs edge-to-edge rule):
                            // a rail boxed in a card clips on the rounded corners.
                            MediaRail(
                                title = kind.title(strings, current, next),
                                items = media.mapNotNull { it.toRailItem() },
                                onSelect = onOpenMedia,
                                cardWidth = if (kind.isFeatured) {
                                    MediaCoverCardCoverWidth * FeaturedCoverScale
                                } else {
                                    MediaCoverCardCoverWidth
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The leading, featured rail of each selector. */
private val DashboardSectionKind.isFeatured: Boolean
    get() = this == DashboardSectionKind.SEASONAL || this == DashboardSectionKind.NEW_RELEASES_MANGA

/** Rail title; the season rails read "Fall 2026" / "Winter 2027" (localized). */
private fun DashboardSectionKind.title(
    strings: LanguageStrings,
    current: MediaSeasonYear,
    next: MediaSeasonYear,
): String = when (this) {
    DashboardSectionKind.SEASONAL -> "${current.season.label(strings)} ${current.year}"
    DashboardSectionKind.UPCOMING_SEASON -> "${next.season.label(strings)} ${next.year}"
    DashboardSectionKind.TRENDING_ANIME -> strings.discoverTrendingAnimeTitle
    DashboardSectionKind.POPULAR_ANIME -> strings.discoverAllTimePopularAnimeTitle
    DashboardSectionKind.POPULAR_MOVIE -> strings.discoverAllTimePopularMovieTitle
    DashboardSectionKind.BEST_ANIME -> strings.discoverBestRatedAnimeTitle
    DashboardSectionKind.BEST_MOVIE -> strings.discoverBestRatedMovieTitle
    DashboardSectionKind.BEST_ANIME_NOW -> strings.discoverBestRatedAnimeNowTitle
    DashboardSectionKind.BEST_MOVIE_NOW -> strings.discoverBestRatedMovieNowTitle
    DashboardSectionKind.NEW_RELEASES_MANGA -> strings.discoverNewReleasesMangaTitle
    DashboardSectionKind.TRENDING_MANGA -> strings.discoverTrendingMangaTitle
    DashboardSectionKind.POPULAR_MANGA -> strings.discoverAllTimePopularMangaTitle
    DashboardSectionKind.POPULAR_LIGHT_NOVEL -> strings.discoverAllTimePopularLightNovelTitle
    DashboardSectionKind.POPULAR_MANHWA -> strings.discoverAllTimePopularManhwaTitle
    DashboardSectionKind.POPULAR_MANHUA -> strings.discoverAllTimePopularManhuaTitle
    DashboardSectionKind.BEST_MANGA -> strings.discoverBestRatedMangaTitle
    DashboardSectionKind.BEST_LIGHT_NOVEL -> strings.discoverBestRatedLightNovelTitle
    DashboardSectionKind.BEST_MANHWA -> strings.discoverBestRatedManhwaTitle
    DashboardSectionKind.BEST_MANHUA -> strings.discoverBestRatedManhuaTitle
}

/** Catalog items with a missing id cannot navigate, so they never reach a rail. */
private fun MediaItem.toRailItem(): MediaRailItem? = id?.let {
    MediaRailItem(
        id = it,
        title = title,
        image = cover,
        subtitle = meta,
    )
}

/**
 * Non-blocking refresh error: the rails keep their stale content and a compact
 * line with retry sits above them (a failed refresh must not blank the page).
 */
@Composable
private fun DashboardRefreshError(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text(text = retryLabel)
        }
    }
}
