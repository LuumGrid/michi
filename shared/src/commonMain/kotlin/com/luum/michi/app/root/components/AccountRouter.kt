package com.luum.michi.app.root.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.luum.michi.app.account.presentation.AccountFavoritesGridScreen
import com.luum.michi.app.account.presentation.AccountScreen
import com.luum.michi.app.account.presentation.AccountStatsScreen
import com.luum.michi.app.account.domain.model.AccountFavoritesCategory
import com.luum.michi.app.account.presentation.model.AccountProfileDraft
import com.luum.michi.app.account.presentation.state.AccountFavoritesGridStateHolder
import com.luum.michi.app.account.presentation.state.AccountStateHolder
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.ui.BackHandler
import com.luum.michi.app.settings.presentation.SettingsScreen
import com.luum.michi.app.settings.presentation.state.SettingsState
import com.luum.michi.app.root.state.AccountRoute
import com.luum.michi.app.root.state.State

@Composable
internal fun AccountRouter(
    route: AccountRoute,
    profile: AccountProfileDraft,
    settingsState: SettingsState,
    accountState: AccountStateHolder,
    state: State,
    favoritesCategory: AccountFavoritesCategory,
    favoritesGridStateHolder: AccountFavoritesGridStateHolder,
    language: AppLanguage,
    isDarkMode: Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onToggleTheme: () -> Unit,
    onNavigate: (AccountRoute) -> Unit,
    onOpenAnimeList: () -> Unit,
    onOpenMangaList: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    onOpenStaff: (Int) -> Unit,
    onOpenStudio: (Int) -> Unit,
    onOpenFavoritesGrid: (AccountFavoritesCategory) -> Unit,
    onLogout: () -> Unit,
    onBackHandlerChange: (BackHandler?) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    when (route) {
        AccountRoute.ACCOUNT -> {
            AccountScreen(
                stateHolder = accountState,
                username = profile.username,
                bannerUrl = profile.bannerUrl,
                userAvatarUrl = profile.avatarUrl,
                userBio = profile.bio,
                joinedLabel = null,
                onRefresh = accountState::refresh,
                onEditProfileClick = state::openAccountSettings,
                onShareProfileClick = state::openShareProfile,
                onOpenAnimeList = onOpenAnimeList,
                onOpenMangaList = onOpenMangaList,
                onOpenMedia = onOpenMedia,
                onEditMedia = onEditMedia,
                onOpenCharacter = onOpenCharacter,
                onOpenStaff = onOpenStaff,
                onOpenStudio = onOpenStudio,
                onOpenStats = { onNavigate(AccountRoute.STATS) },
                onOpenFavoritesGrid = onOpenFavoritesGrid,
            )
        }

        AccountRoute.SETTINGS -> {
            SettingsScreen(
                settingsState = settingsState,
                language = language,
                onLanguageChange = onLanguageChange,
                isDarkMode = isDarkMode,
                onToggleTheme = onToggleTheme,
                onLogout = onLogout,
                onManageAccount = state::openAccountSettings,
                onHelp = { uriHandler.openUri("https://anilist.co/forum/overview") },
                onBackHandlerChange = onBackHandlerChange,
            )
        }

        AccountRoute.STATS -> {
            AccountStatsScreen(stats = accountState.stats)
        }

        AccountRoute.FAVORITES -> {
            AccountFavoritesGridScreen(
                stateHolder = favoritesGridStateHolder,
                category = favoritesCategory,
                onOpenMedia = onOpenMedia,
                onEditMedia = onEditMedia,
                onOpenCharacter = onOpenCharacter,
                onOpenStaff = onOpenStaff,
                onOpenStudio = onOpenStudio,
            )
        }
    }
}
