package com.luum.michi.app.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.components.AccountFavoriteMediaCard
import com.luum.michi.app.core.platform.components.tabBarClearance
import com.luum.michi.app.account.presentation.components.AccountFavoritePersonCard
import com.luum.michi.app.account.presentation.components.AccountFavoriteSection
import com.luum.michi.app.account.presentation.components.AccountFavoriteStudioCard
import com.luum.michi.app.account.presentation.components.AccountHeader
import com.luum.michi.app.account.presentation.components.AccountStatsRow
import com.luum.michi.app.account.domain.model.AccountFavorites
import com.luum.michi.app.account.domain.model.AccountFavoritesCategory
import com.luum.michi.app.account.domain.model.AccountStats
import com.luum.michi.app.account.presentation.state.AccountStateHolder
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformListLoading
import com.luum.michi.app.core.platform.components.PlatformListMessage
import com.luum.michi.app.core.platform.components.PlatformListMessageTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountScreen(
    stateHolder: AccountStateHolder,
    username: String,
    displayName: String,
    bannerUrl: String?,
    userAvatarUrl: String?,
    userBio: String?,
    joinedLabel: String?,
    onRefresh: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onShareProfileClick: () -> Unit = {},
    onOpenAnimeList: () -> Unit = {},
    onOpenMangaList: () -> Unit = {},
    onOpenMedia: (Int) -> Unit = {},
    onEditMedia: (Int) -> Unit = {},
    onOpenCharacter: (Int) -> Unit = {},
    onOpenStaff: (Int) -> Unit = {},
    onOpenStudio: (Int) -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenFavoritesGrid: (AccountFavoritesCategory) -> Unit = {},
) {
    val strings = LanguageProvider.strings
    PullToRefreshBox(
        isRefreshing = stateHolder.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        AccountContent(
            username = username,
            displayName = displayName,
            bannerUrl = bannerUrl,
            userAvatarUrl = userAvatarUrl,
            userBio = userBio,
            joinedLabel = joinedLabel,
            stats = stateHolder.stats,
            favorites = stateHolder.favorites,
            isLoading = stateHolder.isLoading,
            error = stateHolder.error?.let { strings.networkErrorMessage(it) },
            onEditProfileClick = onEditProfileClick,
            onShareProfileClick = onShareProfileClick,
            onOpenAnimeList = onOpenAnimeList,
            onOpenMangaList = onOpenMangaList,
            onOpenMedia = onOpenMedia,
            onEditMedia = onEditMedia,
            onOpenCharacter = onOpenCharacter,
            onOpenStaff = onOpenStaff,
            onOpenStudio = onOpenStudio,
            onOpenStats = onOpenStats,
            onOpenFavoritesGrid = onOpenFavoritesGrid,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountContent(
    username: String,
    displayName: String,
    bannerUrl: String?,
    userAvatarUrl: String?,
    userBio: String?,
    joinedLabel: String?,
    stats: AccountStats,
    favorites: AccountFavorites,
    isLoading: Boolean,
    error: String?,
    onEditProfileClick: () -> Unit,
    onShareProfileClick: () -> Unit,
    onOpenAnimeList: () -> Unit,
    onOpenMangaList: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    onOpenStaff: (Int) -> Unit,
    onOpenStudio: (Int) -> Unit,
    onOpenStats: () -> Unit,
    onOpenFavoritesGrid: (AccountFavoritesCategory) -> Unit,
) {
    val strings = LanguageProvider.strings
    // Igual que en Anime/Manga: mientras no haya stats cargadas, no consideramos
    // que ya tenemos datos que mostrar (ver ShellScreen, que usa el mismo criterio
    // para decidir si dispara la carga inicial).
    val hasData = stats.animeCount > 0 || stats.mangaCount > 0

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && !hasData -> PlatformListLoading(strings.listsLoadingLabel)
            error != null && !hasData -> PlatformListMessage(
                title = strings.listsErrorLabel,
                subtitle = error,
                tone = PlatformListMessageTone.Error,
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(bottom = tabBarClearance()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    AccountHeader(
                        username = username,
                        displayName = displayName,
                        bannerUrl = bannerUrl,
                        userAvatarUrl = userAvatarUrl,
                        userBio = userBio,
                        joinedLabel = joinedLabel,
                        onEditProfileClick = onEditProfileClick,
                        onShareProfileClick = onShareProfileClick,
                    )
                }

                item {
                    AccountStatsRow(
                        stats = stats,
                        onAnimeClick = onOpenAnimeList,
                        onMangaClick = onOpenMangaList,
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenStats() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = PlatformIcons.Stats,
                            contentDescription = strings.accountStatsTitle,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = strings.accountStatsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            painter = PlatformIcons.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    AccountFavoriteSection(
                        title = strings.accountFavoriteAnimeTitle,
                        items = favorites.anime,
                        onSeeAll = { onOpenFavoritesGrid(AccountFavoritesCategory.ANIME) },
                        itemKey = { it.id },
                    ) {
                        AccountFavoriteMediaCard(
                            media = it,
                            onClick = { onOpenMedia(it.id) },
                            onLongClick = { onEditMedia(it.id) },
                        )
                    }
                }

                item {
                    AccountFavoriteSection(
                        title = strings.accountFavoriteMangaTitle,
                        items = favorites.manga,
                        onSeeAll = { onOpenFavoritesGrid(AccountFavoritesCategory.MANGA) },
                        itemKey = { it.id },
                    ) {
                        AccountFavoriteMediaCard(
                            media = it,
                            onClick = { onOpenMedia(it.id) },
                            onLongClick = { onEditMedia(it.id) },
                        )
                    }
                }

                item {
                    AccountFavoriteSection(
                        title = strings.accountFavoriteCharactersTitle,
                        items = favorites.characters,
                        onSeeAll = { onOpenFavoritesGrid(AccountFavoritesCategory.CHARACTERS) },
                        itemKey = { it.id },
                    ) {
                        AccountFavoritePersonCard(
                            person = it,
                            onClick = { onOpenCharacter(it.id) },
                        )
                    }
                }

                item {
                    AccountFavoriteSection(
                        title = strings.accountFavoriteStaffTitle,
                        items = favorites.staff,
                        onSeeAll = { onOpenFavoritesGrid(AccountFavoritesCategory.STAFF) },
                        itemKey = { it.id },
                    ) {
                        AccountFavoritePersonCard(
                            person = it,
                            onClick = { onOpenStaff(it.id) },
                        )
                    }
                }

                item {
                    AccountFavoriteSection(
                        title = strings.accountFavoriteStudiosTitle,
                        items = favorites.studios,
                        onSeeAll = { onOpenFavoritesGrid(AccountFavoritesCategory.STUDIOS) },
                        itemKey = { it.id },
                    ) {
                        AccountFavoriteStudioCard(
                            studio = it,
                            onClick = { onOpenStudio(it.id) },
                        )
                    }
                }
            }
        }
    }
}
