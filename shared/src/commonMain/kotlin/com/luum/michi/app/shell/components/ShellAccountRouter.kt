package com.luum.michi.app.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.luum.michi.app.account.presentation.AccountEditProfileScreen
import com.luum.michi.app.account.presentation.AccountFavoritesGridScreen
import com.luum.michi.app.account.presentation.AccountScreen
import com.luum.michi.app.account.presentation.AccountShareProfileScreen
import com.luum.michi.app.account.presentation.AccountStatsScreen
import com.luum.michi.app.account.presentation.model.AccountFavorites
import com.luum.michi.app.account.presentation.model.AccountFavoritesCategory
import com.luum.michi.app.account.presentation.model.AccountProfileDraft
import com.luum.michi.app.account.presentation.model.AccountStats
import com.luum.michi.app.account.presentation.state.AccountFavoritesGridStateHolder
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.platform.PlatformBackHandler
import com.luum.michi.app.settings.presentation.SettingsScreen
import com.luum.michi.app.settings.presentation.state.SettingsState
import com.luum.michi.app.shell.state.ShellAccountRoute

@Composable
internal fun ShellAccountRouter(
    route: ShellAccountRoute,
    profile: AccountProfileDraft,
    settingsState: SettingsState,
    accountStats: AccountStats,
    accountFavorites: AccountFavorites,
    accountIsRefreshing: Boolean,
    onAccountRefresh: () -> Unit,
    favoritesCategory: AccountFavoritesCategory,
    favoritesGridStateHolder: AccountFavoritesGridStateHolder,
    language: AppLanguage,
    isDarkMode: Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onToggleTheme: () -> Unit,
    onNavigate: (ShellAccountRoute) -> Unit,
    onOpenAnimationList: () -> Unit,
    onOpenReadingList: () -> Unit,
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
                username = profile.username,
                displayName = profile.displayName,
                bannerUrl = profile.bannerUrl,
                userAvatarUrl = profile.avatarUrl,
                userBio = profile.bio,
                joinedLabel = null,
                stats = accountStats,
                favorites = accountFavorites,
                isRefreshing = accountIsRefreshing,
                onRefresh = onAccountRefresh,
                onEditProfileClick = { onNavigate(ShellAccountRoute.EDIT_PROFILE) },
                onShareProfileClick = { onNavigate(ShellAccountRoute.SHARE_PROFILE) },
                onOpenAnimationList = onOpenAnimationList,
                onOpenReadingList = onOpenReadingList,
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
                onManageAccount = { onNavigate(ShellAccountRoute.EDIT_PROFILE) },
                onHelp = { uriHandler.openUri("https://anilist.co/forum/overview") },
                onBackHandlerChange = onBackHandlerChange,
            )
        }

        ShellAccountRoute.EDIT_PROFILE -> {
            AccountEditProfileScreen(initialDraft = profile)
        }

        ShellAccountRoute.SHARE_PROFILE -> {
            AccountShareProfileScreen(
                username = profile.username,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
            )
        }

        ShellAccountRoute.STATS -> {
            AccountStatsScreen(stats = accountStats)
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
