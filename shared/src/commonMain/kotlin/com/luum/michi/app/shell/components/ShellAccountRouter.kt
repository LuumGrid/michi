package com.luum.michi.app.shell.components

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
import com.luum.michi.app.core.platform.PlatformBackHandler
import com.luum.michi.app.settings.presentation.SettingsScreen
import com.luum.michi.app.settings.presentation.state.SettingsState
import com.luum.michi.app.shell.state.ShellAccountRoute
import com.luum.michi.app.shell.state.ShellState

@Composable
internal fun ShellAccountRouter(
    route: ShellAccountRoute,
    profile: AccountProfileDraft,
    settingsState: SettingsState,
    accountState: AccountStateHolder,
    shellState: ShellState,
    favoritesCategory: AccountFavoritesCategory,
    favoritesGridStateHolder: AccountFavoritesGridStateHolder,
    language: AppLanguage,
    isDarkMode: Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onToggleTheme: () -> Unit,
    onNavigate: (ShellAccountRoute) -> Unit,
    onOpenAnimeList: () -> Unit,
    onOpenMangaList: () -> Unit,
    onOpenMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onOpenCharacter: (Int) -> Unit,
    onOpenStaff: (Int) -> Unit,
    onOpenStudio: (Int) -> Unit,
    onOpenFavoritesGrid: (AccountFavoritesCategory) -> Unit,
    onLogout: () -> Unit,
    onBackHandlerChange: (PlatformBackHandler?) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    when (route) {
        ShellAccountRoute.ACCOUNT -> {
            AccountScreen(
                stateHolder = accountState,
                username = profile.username,
                displayName = profile.displayName,
                bannerUrl = profile.bannerUrl,
                userAvatarUrl = profile.avatarUrl,
                userBio = profile.bio,
                joinedLabel = null,
                onRefresh = accountState::refresh,
                onEditProfileClick = shellState::openAccountSettings,
                onShareProfileClick = shellState::openShareProfile,
                onOpenAnimeList = onOpenAnimeList,
                onOpenMangaList = onOpenMangaList,
                onOpenMedia = onOpenMedia,
                onEditMedia = onEditMedia,
                onOpenCharacter = onOpenCharacter,
                onOpenStaff = onOpenStaff,
                onOpenStudio = onOpenStudio,
                onOpenStats = { onNavigate(ShellAccountRoute.STATS) },
                onOpenFavoritesGrid = onOpenFavoritesGrid,
            )
        }

        ShellAccountRoute.SETTINGS -> {
            SettingsScreen(
                settingsState = settingsState,
                language = language,
                onLanguageChange = onLanguageChange,
                isDarkMode = isDarkMode,
                onToggleTheme = onToggleTheme,
                onLogout = onLogout,
                onManageAccount = shellState::openAccountSettings,
                onHelp = { uriHandler.openUri("https://anilist.co/forum/overview") },
                onBackHandlerChange = onBackHandlerChange,
            )
        }

        ShellAccountRoute.STATS -> {
            AccountStatsScreen(stats = accountState.stats)
        }

        ShellAccountRoute.FAVORITES -> {
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
