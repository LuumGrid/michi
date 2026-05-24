package com.luum.michi.app.browse.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.browse.presentation.state.BrowseStateHolder
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.components.PlatformHomeMediaRail
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone

@Composable
internal fun BrowseScreen(
    stateHolder: BrowseStateHolder,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
) {
    val strings = LanguageProvider.strings
    val hasContent = stateHolder.popularThisSeason.isNotEmpty() ||
        stateHolder.upcomingNextSeason.isNotEmpty() ||
        stateHolder.allTimePopularAnime.isNotEmpty() ||
        stateHolder.allTimePopularManga.isNotEmpty() ||
        stateHolder.topAnime.isNotEmpty() ||
        stateHolder.topManga.isNotEmpty()

    when {
        stateHolder.isLoading && !hasContent -> PlatformListLoading(strings.listsLoadingLabel)
        stateHolder.error != null && !hasContent -> PlatformListMessage(
            title = strings.listsErrorLabel,
            subtitle = stateHolder.error,
            tone = PlatformListMessageTone.Error,
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            if (stateHolder.popularThisSeason.isNotEmpty()) {
                item {
                    PlatformHomeMediaRail(
                        title = strings.browsePopularThisSeasonTitle,
                        items = stateHolder.popularThisSeason,
                        onItemClick = onOpenMedia,
                        onItemLongClick = onEditMedia,
                    )
                }
            }
            if (stateHolder.upcomingNextSeason.isNotEmpty()) {
                item {
                    PlatformHomeMediaRail(
                        title = strings.browseUpcomingNextSeasonTitle,
                        items = stateHolder.upcomingNextSeason,
                        onItemClick = onOpenMedia,
                        onItemLongClick = onEditMedia,
                    )
                }
            }
            if (stateHolder.allTimePopularAnime.isNotEmpty()) {
                item {
                    PlatformHomeMediaRail(
                        title = strings.browseAllTimePopularAnimeTitle,
                        items = stateHolder.allTimePopularAnime,
                        onItemClick = onOpenMedia,
                        onItemLongClick = onEditMedia,
                    )
                }
            }
            if (stateHolder.allTimePopularManga.isNotEmpty()) {
                item {
                    PlatformHomeMediaRail(
                        title = strings.browseAllTimePopularMangaTitle,
                        items = stateHolder.allTimePopularManga,
                        onItemClick = onOpenMedia,
                        onItemLongClick = onEditMedia,
                    )
                }
            }
            if (stateHolder.topAnime.isNotEmpty()) {
                item {
                    PlatformHomeMediaRail(
                        title = strings.browseTopAnimeTitle,
                        items = stateHolder.topAnime,
                        onItemClick = onOpenMedia,
                        onItemLongClick = onEditMedia,
                    )
                }
            }
            if (stateHolder.topManga.isNotEmpty()) {
                item {
                    PlatformHomeMediaRail(
                        title = strings.browseTopMangaTitle,
                        items = stateHolder.topManga,
                        onItemClick = onOpenMedia,
                        onItemLongClick = onEditMedia,
                    )
                }
            }
        }
    }
}
