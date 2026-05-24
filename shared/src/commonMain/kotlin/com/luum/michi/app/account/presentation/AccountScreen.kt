package com.luum.michi.app.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.components.AccountFavoriteMediaCard
import com.luum.michi.app.account.presentation.components.AccountFavoritePersonCard
import com.luum.michi.app.account.presentation.components.AccountFavoriteSection
import com.luum.michi.app.account.presentation.components.AccountFavoriteStudioCard
import com.luum.michi.app.account.presentation.components.AccountHeader
import com.luum.michi.app.account.presentation.components.AccountStatsRow
import com.luum.michi.app.account.presentation.model.AccountFavorites
import com.luum.michi.app.account.presentation.model.AccountStats
import com.luum.michi.app.core.language.LanguageProvider

private val EmptyAccountStats = AccountStats(
    animeCount = 0,
    mangaCount = 0,
    followingCount = 0,
    followersCount = 0,
)

private val EmptyAccountFavorites = AccountFavorites(
    anime = emptyList(),
    manga = emptyList(),
    characters = emptyList(),
    staff = emptyList(),
    studios = emptyList(),
)

@Composable
internal fun AccountScreen(
    username: String,
    displayName: String,
    bannerUrl: String?,
    userAvatarUrl: String?,
    userBio: String?,
    joinedLabel: String?,
    stats: AccountStats = EmptyAccountStats,
    favorites: AccountFavorites = EmptyAccountFavorites,
    onEditProfileClick: () -> Unit = {},
    onShareProfileClick: () -> Unit = {},
    onOpenAnimationList: () -> Unit = {},
    onOpenReadingList: () -> Unit = {},
    onOpenMedia: (Int) -> Unit = {},
    onEditMedia: (Int) -> Unit = {},
) {
    val strings = LanguageProvider.strings

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(bottom = 24.dp),
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
                onAnimeClick = onOpenAnimationList,
                onMangaClick = onOpenReadingList,
            )
        }

        item {
            AccountFavoriteSection(
                title = strings.accountFavoriteAnimeTitle,
                items = favorites.anime,
                onSeeMore = onOpenAnimationList,
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
                onSeeMore = onOpenReadingList,
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
                onSeeMore = { },
                itemKey = { it.id },
            ) { AccountFavoritePersonCard(person = it) }
        }

        item {
            AccountFavoriteSection(
                title = strings.accountFavoriteStaffTitle,
                items = favorites.staff,
                onSeeMore = { },
                itemKey = { it.id },
            ) { AccountFavoritePersonCard(person = it) }
        }

        item {
            AccountFavoriteSection(
                title = strings.accountFavoriteStudiosTitle,
                items = favorites.studios,
                onSeeMore = { },
                itemKey = { it.id },
            ) { AccountFavoriteStudioCard(studio = it) }
        }
    }
}
