package com.luum.michi.app.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformHomeMediaRail
import com.luum.michi.app.dashboard.presentation.state.DashboardStateHolder

/** Identifica cada rail del Dashboard para que el "Ver todo" del header decida su destino. */
internal enum class DashboardRail {
    THIS_SEASON,
    TRENDING_ANIME,
    TRENDING_MANGA,
    UPCOMING_NEXT_SEASON,
    ALL_TIME_POPULAR_ANIME,
    ALL_TIME_POPULAR_MANGA,
    TOP_ANIME,
    TOP_MANGA,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScreen(
    stateHolder: DashboardStateHolder,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onSeeAll: (DashboardRail) -> Unit,
    onRefresh: () -> Unit = { stateHolder.load(forceRefresh = true) },
) {
    val strings = LanguageProvider.strings

    PullToRefreshBox(
        isRefreshing = stateHolder.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {

        if (stateHolder.thisSeason.isNotEmpty()) {
            item {
                PlatformHomeMediaRail(
                    title = strings.exploreThisSeasonTitle,
                    items = stateHolder.thisSeason,
                    onItemClick = onOpenMedia,
                    onItemLongClick = onEditMedia,
                    onSeeAll = { onSeeAll(DashboardRail.THIS_SEASON) },
                )
            }
        }

        if (stateHolder.trendingAnimation.isNotEmpty()) {
            item {
                PlatformHomeMediaRail(
                    title = strings.homeTrendingAnimationTitle,
                    items = stateHolder.trendingAnimation,
                    onItemClick = onOpenMedia,
                    onItemLongClick = onEditMedia,
                    onSeeAll = { onSeeAll(DashboardRail.TRENDING_ANIME) },
                )
            }
        }

        if (stateHolder.trendingReading.isNotEmpty()) {
            item {
                PlatformHomeMediaRail(
                    title = strings.homeTrendingReadingTitle,
                    items = stateHolder.trendingReading,
                    onItemClick = onOpenMedia,
                    onItemLongClick = onEditMedia,
                    onSeeAll = { onSeeAll(DashboardRail.TRENDING_MANGA) },
                )
            }
        }

        if (stateHolder.upcomingNextSeason.isNotEmpty()) {
            item {
                PlatformHomeMediaRail(
                    title = strings.exploreUpcomingNextSeasonTitle,
                    items = stateHolder.upcomingNextSeason,
                    onItemClick = onOpenMedia,
                    onItemLongClick = onEditMedia,
                    onSeeAll = { onSeeAll(DashboardRail.UPCOMING_NEXT_SEASON) },
                )
            }
        }

        if (stateHolder.allTimePopularAnime.isNotEmpty()) {
            item {
                PlatformHomeMediaRail(
                    title = strings.exploreAllTimePopularAnimeTitle,
                    items = stateHolder.allTimePopularAnime,
                    onItemClick = onOpenMedia,
                    onItemLongClick = onEditMedia,
                    onSeeAll = { onSeeAll(DashboardRail.ALL_TIME_POPULAR_ANIME) },
                )
            }
        }

        if (stateHolder.allTimePopularManga.isNotEmpty()) {
            item {
                PlatformHomeMediaRail(
                    title = strings.exploreAllTimePopularMangaTitle,
                    items = stateHolder.allTimePopularManga,
                    onItemClick = onOpenMedia,
                    onItemLongClick = onEditMedia,
                    onSeeAll = { onSeeAll(DashboardRail.ALL_TIME_POPULAR_MANGA) },
                )
            }
        }

        if (stateHolder.topAnime.isNotEmpty()) {
            item {
                PlatformHomeMediaRail(
                    title = strings.exploreTopAnimeTitle,
                    items = stateHolder.topAnime,
                    onItemClick = onOpenMedia,
                    onItemLongClick = onEditMedia,
                    onSeeAll = { onSeeAll(DashboardRail.TOP_ANIME) },
                )
            }
        }

        if (stateHolder.topManga.isNotEmpty()) {
            item {
                PlatformHomeMediaRail(
                    title = strings.exploreTopMangaTitle,
                    items = stateHolder.topManga,
                    onItemClick = onOpenMedia,
                    onItemLongClick = onEditMedia,
                    onSeeAll = { onSeeAll(DashboardRail.TOP_MANGA) },
                )
            }
        }
    }
    }
}
