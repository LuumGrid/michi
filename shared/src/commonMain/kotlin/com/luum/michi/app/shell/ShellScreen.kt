package com.luum.michi.app.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.presentation.AccountEditProfileScreen
import com.luum.michi.app.account.presentation.AccountProfileDraft
import com.luum.michi.app.account.presentation.AccountScreen
import com.luum.michi.app.account.presentation.AccountShareProfileScreen
import com.luum.michi.app.animation.presentation.AnimationListSection
import com.luum.michi.app.animation.presentation.AnimationScreen
import com.luum.michi.app.animation.presentation.AnimationSectionPlatformChips
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.MichiAppName
import com.luum.michi.app.core.platform.PlatformBackHandler
import com.luum.michi.app.core.platform.MichiBrand
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.PlatformSystemBackHandler
import com.luum.michi.app.core.platform.components.PlatformTopBar
import com.luum.michi.app.discovery.presentation.DiscoveryScreen
import com.luum.michi.app.reading.presentation.ReadingListSection
import com.luum.michi.app.reading.presentation.ReadingScreen
import com.luum.michi.app.reading.presentation.ReadingSectionPlatformChips
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
) {
    val strings = LanguageProvider.strings
    var selectedTab by remember { mutableStateOf(ShellBottomTab.HOME) }
    var selectedAnimationSection by remember { mutableStateOf(AnimationListSection.ALL) }
    var selectedReadingSection by remember { mutableStateOf(ReadingListSection.ALL) }
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        snapAnimationSpec = null,
        flingAnimationSpec = null,
    )
    val chipsFractionState = remember {
        derivedStateOf {
            val limit = scrollBehavior.state.heightOffsetLimit
            if (limit < 0f) (scrollBehavior.state.heightOffset / limit).coerceIn(0f, 1f) else 0f
        }
    }
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
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            ) {
                PlatformTopBar(
                    title = {
                        Text(
                            text = when {
                                selectedTab == ShellBottomTab.ACCOUNT && accountRoute == ShellAccountRoute.SETTINGS -> strings.settingsAction
                                selectedTab == ShellBottomTab.ACCOUNT && accountRoute == ShellAccountRoute.EDIT_PROFILE -> strings.accountEditProfileAction
                                selectedTab == ShellBottomTab.ACCOUNT && accountRoute == ShellAccountRoute.SHARE_PROFILE -> strings.accountShareProfileAction
                                selectedTab == ShellBottomTab.ACCOUNT -> "${currentProfile.displayName} | @${currentProfile.username}"
                                else -> selectedTab.label(strings)
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        when (selectedTab) {
                            ShellBottomTab.HOME,
                            ShellBottomTab.ANIMATION,
                            ShellBottomTab.READING -> {
                                IconButton(onClick = { /* TODO: filtros */ }) {
                                    Icon(
                                        painter = PlatformIcons.FilterList,
                                        contentDescription = strings.filterAction,
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
                            ShellBottomTab.HOME,
                            ShellBottomTab.ANIMATION,
                            ShellBottomTab.READING -> {
                                IconButton(onClick = { /* TODO: notificaciones */ }) {
                                    Icon(
                                        painter = PlatformIcons.Mood,
                                        contentDescription = strings.notificationsAction,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }

                            ShellBottomTab.ACCOUNT -> {
                                if (!isAccountDetail) {
                                    IconButton(onClick = { /* TODO: notificaciones */ }) {
                                        Icon(
                                            painter = PlatformIcons.Mood,
                                            contentDescription = strings.notificationsAction,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets.statusBars,
                )

                if (selectedTab == ShellBottomTab.ANIMATION) {
                    AnimationSectionPlatformChips(
                        selected = selectedAnimationSection,
                        onSelect = { selectedAnimationSection = it },
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val naturalH = placeable.height
                                val fraction = chipsFractionState.value
                                val visibleH = (naturalH * (1f - fraction)).toInt().coerceAtLeast(0)
                                layout(placeable.width, visibleH) {
                                    placeable.place(0, -(naturalH * fraction).toInt())
                                }
                            }
                            .graphicsLayer { alpha = 1f - chipsFractionState.value },
                    )
                }

                if (selectedTab == ShellBottomTab.READING) {
                    ReadingSectionPlatformChips(
                        selected = selectedReadingSection,
                        onSelect = { selectedReadingSection = it },
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val naturalH = placeable.height
                                val fraction = chipsFractionState.value
                                val visibleH = (naturalH * (1f - fraction)).toInt().coerceAtLeast(0)
                                layout(placeable.width, visibleH) {
                                    placeable.place(0, -(naturalH * fraction).toInt())
                                }
                            }
                            .graphicsLayer { alpha = 1f - chipsFractionState.value },
                    )
                }
            }
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
                .padding(
                    top = if (selectedTab == ShellBottomTab.ACCOUNT) 0.dp else contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
        ) {
            when (selectedTab) {
                ShellBottomTab.HOME -> DiscoveryScreen()
                ShellBottomTab.ANIMATION -> AnimationScreen(
                    selectedSection = selectedAnimationSection,
                    scrollBehavior = scrollBehavior,
                )
                ShellBottomTab.READING -> ReadingScreen(
                    selectedSection = selectedReadingSection,
                    scrollBehavior = scrollBehavior,
                )
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
