package com.luum.michi.app.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.AccountEditProfileScreen
import com.luum.michi.app.account.presentation.AccountProfileDraft
import com.luum.michi.app.account.presentation.AccountScreen
import com.luum.michi.app.account.presentation.AccountShareProfileScreen
import com.luum.michi.app.animation.presentation.AnimationScreen
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.MichiAppName
import com.luum.michi.app.core.platform.PlatformBackHandler
import com.luum.michi.app.core.platform.MichiBrand
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.PlatformSystemBackHandler
import com.luum.michi.app.core.platform.components.PlatformTopBar
import com.luum.michi.app.discovery.presentation.DiscoveryScreen
import com.luum.michi.app.illustration.presentation.IllustrationScreen
import com.luum.michi.app.search.presentation.SearchScreen
import com.luum.michi.app.settings.presentation.SettingsScreen
import com.luum.michi.app.shell.components.ShellBottomNavBar
import com.luum.michi.app.shell.components.ShellBottomTab
import com.luum.michi.app.shell.components.label

private enum class ShellAccountRoute {
    ACCOUNT,
    SETTINGS,
    EDIT_PROFILE,
    SHARE_PROFILE,
}

@Composable
fun ShellScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
) {
    val strings = LanguageProvider.strings
    var selectedTab by remember { mutableStateOf(ShellBottomTab.DISCOVERY) }
    var accountRoute by remember { mutableStateOf(ShellAccountRoute.ACCOUNT) }
    var topBarBackHandler by remember { mutableStateOf<PlatformBackHandler?>(null) }
    var currentProfile by remember {
        mutableStateOf(
            AccountProfileDraft(
                username = "psyxho_skull",
                displayName = MichiAppName,
                avatarUrl = null,
                bio = "Anime, manga y listas en Michi.",
                email = "",
            ),
        )
    }
    val isAccountDetail = selectedTab == ShellBottomTab.ACCOUNT && accountRoute != ShellAccountRoute.ACCOUNT
    val handleAccountBack: PlatformBackHandler = {
        val handler = topBarBackHandler
        if (handler != null) {
            handler()
        } else {
            accountRoute = ShellAccountRoute.ACCOUNT
        }
    }

    PlatformSystemBackHandler(
        enabled = isAccountDetail,
        onBack = handleAccountBack,
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            PlatformTopBar(
                title = {
                    Text(
                        text = when {
                            selectedTab == ShellBottomTab.ACCOUNT && accountRoute == ShellAccountRoute.SETTINGS -> strings.settingsAction
                            selectedTab == ShellBottomTab.ACCOUNT && accountRoute == ShellAccountRoute.EDIT_PROFILE -> "Editar perfil"
                            selectedTab == ShellBottomTab.ACCOUNT && accountRoute == ShellAccountRoute.SHARE_PROFILE -> "Compartir perfil"
                            selectedTab == ShellBottomTab.ACCOUNT -> "${currentProfile.displayName} | @${currentProfile.username}"
                            else -> selectedTab.label()
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    when (selectedTab) {
                        ShellBottomTab.DISCOVERY,
                        ShellBottomTab.SEARCH,
                        ShellBottomTab.ANIMATION,
                        ShellBottomTab.ILLUSTRATION -> {
                            IconButton(onClick = { /* TODO: filtros */ }) {
                                Icon(
                                    painter = PlatformIcons.FilterList,
                                    contentDescription = "Filtrar",
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        ShellBottomTab.ACCOUNT -> {
                            IconButton(
                                onClick = {
                                    if (isAccountDetail) {
                                        handleAccountBack()
                                    } else {
                                        accountRoute = ShellAccountRoute.SETTINGS
                                    }
                                },
                            ) {
                                Icon(
                                    painter = if (isAccountDetail) PlatformIcons.ArrowBack else PlatformIcons.Settings,
                                    contentDescription = if (isAccountDetail) strings.tabAccount else strings.settingsAction,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }
                },
                actions = {
                    when (selectedTab) {
                        ShellBottomTab.DISCOVERY,
                        ShellBottomTab.ANIMATION,
                        ShellBottomTab.ILLUSTRATION -> {
                            IconButton(onClick = { /* TODO: notificaciones */ }) {
                                Icon(
                                    painter = PlatformIcons.Mood,
                                    contentDescription = "Notificaciones",
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        ShellBottomTab.ACCOUNT -> {
                            if (!isAccountDetail) {
                                IconButton(onClick = { /* TODO: notificaciones */ }) {
                                    Icon(
                                        painter = PlatformIcons.Mood,
                                        contentDescription = "Notificaciones",
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                        }

                        ShellBottomTab.SEARCH -> Unit
                    }
                },
                windowInsets = WindowInsets.statusBars,
            )
        },
        bottomBar = {
            if (!isAccountDetail) {
                ShellBottomNavBar(
                    selected = selectedTab,
                    onSelect = {
                        selectedTab = it
                        if (it != ShellBottomTab.ACCOUNT) {
                            accountRoute = ShellAccountRoute.ACCOUNT
                            topBarBackHandler = null
                        }
                    },
                )
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = contentPadding.calculateBottomPadding()),
        ) {
            when (selectedTab) {
                ShellBottomTab.DISCOVERY -> DiscoveryScreen()
                ShellBottomTab.SEARCH -> SearchScreen()
                ShellBottomTab.ANIMATION -> AnimationScreen()
                ShellBottomTab.ILLUSTRATION -> IllustrationScreen()
                ShellBottomTab.ACCOUNT -> {
                    when (accountRoute) {
                        ShellAccountRoute.ACCOUNT -> {
                            AccountScreen(
                                brand = MichiBrand.ANIMATION,
                                username = currentProfile.username,
                                displayName = currentProfile.displayName,
                                userAvatarUrl = currentProfile.avatarUrl,
                                userBio = currentProfile.bio,
                                onEditProfileClick = { accountRoute = ShellAccountRoute.EDIT_PROFILE },
                                onShareProfileClick = { accountRoute = ShellAccountRoute.SHARE_PROFILE },
                            )
                        }

                        ShellAccountRoute.SETTINGS -> {
                            SettingsScreen(
                                language = language,
                                onLanguageChange = onLanguageChange,
                                isDarkMode = isDarkMode,
                                onToggleTheme = onToggleTheme,
                                onAddAccount = { },
                                onLogout = { },
                                onBackHandlerChange = { topBarBackHandler = it },
                            )
                        }

                        ShellAccountRoute.EDIT_PROFILE -> {
                            AccountEditProfileScreen(
                                initialDraft = currentProfile,
                                onSave = { draft ->
                                    currentProfile = draft
                                    accountRoute = ShellAccountRoute.ACCOUNT
                                },
                                onBackHandlerChange = { topBarBackHandler = it },
                            )
                        }

                        ShellAccountRoute.SHARE_PROFILE -> {
                            AccountShareProfileScreen(
                                username = currentProfile.username,
                                displayName = currentProfile.displayName,
                                avatarUrl = currentProfile.avatarUrl,
                            )
                        }
                    }
                }
            }
        }
    }
}
