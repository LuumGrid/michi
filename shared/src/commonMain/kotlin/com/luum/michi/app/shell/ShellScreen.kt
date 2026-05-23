package com.luum.michi.app.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.luum.michi.app.animation.presentation.AnimationScreen
import com.luum.michi.app.animation.presentation.components.AnimationSectionChips
import com.luum.michi.app.animation.presentation.state.rememberAnimationListStateHolder
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformSystemBackHandler
import com.luum.michi.app.discovery.presentation.DiscoveryScreen
import com.luum.michi.app.reading.presentation.ReadingScreen
import com.luum.michi.app.reading.presentation.components.ReadingSectionChips
import com.luum.michi.app.reading.presentation.state.rememberReadingListStateHolder
import com.luum.michi.app.settings.presentation.state.rememberSettingsState
import com.luum.michi.app.shell.components.ShellAccountRouter
import com.luum.michi.app.shell.components.ShellBottomNavBar
import com.luum.michi.app.shell.components.ShellBottomTab
import com.luum.michi.app.shell.components.ShellTopBar
import com.luum.michi.app.shell.components.label
import com.luum.michi.app.shell.components.shellCollapsibleChipsModifier
import com.luum.michi.app.shell.state.ShellAccountRoute
import com.luum.michi.app.shell.state.rememberShellState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
) {
    val strings = LanguageProvider.strings
    val shellState = rememberShellState()
    val animationState = rememberAnimationListStateHolder()
    val readingState = rememberReadingListStateHolder()
    val settingsState = rememberSettingsState()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val chipsFraction by remember {
        derivedStateOf {
            val limit = scrollBehavior.state.heightOffsetLimit
            if (limit < 0f) (scrollBehavior.state.heightOffset / limit).coerceIn(0f, 1f) else 0f
        }
    }

    PlatformSystemBackHandler(
        enabled = shellState.isAccountDetail,
        onBack = shellState::handleAccountBack,
    )
    PlatformSystemBackHandler(
        enabled = shellState.isSearchTab && shellState.isSearchActive,
        onBack = shellState::closeSearch,
    )

    val titleText = when {
        shellState.selectedTab == ShellBottomTab.ACCOUNT &&
            shellState.accountRoute == ShellAccountRoute.SETTINGS -> strings.settingsAction
        shellState.selectedTab == ShellBottomTab.ACCOUNT &&
            shellState.accountRoute == ShellAccountRoute.EDIT_PROFILE -> strings.accountEditProfileAction
        shellState.selectedTab == ShellBottomTab.ACCOUNT &&
            shellState.accountRoute == ShellAccountRoute.SHARE_PROFILE -> strings.accountShareProfileAction
        shellState.selectedTab == ShellBottomTab.ACCOUNT ->
            "${shellState.currentProfile.displayName} | @${shellState.currentProfile.username}"
        else -> shellState.selectedTab.label(strings)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ShellTopBar(
                selectedTab = shellState.selectedTab,
                isAccountDetail = shellState.isAccountDetail,
                isSearchActive = shellState.isSearchActive,
                isSearchTab = shellState.isSearchTab,
                searchQuery = shellState.searchQuery,
                titleText = titleText,
                scrollBehavior = scrollBehavior,
                onOpenSearch = shellState::openSearch,
                onCloseSearch = shellState::closeSearch,
                onSearchQueryChange = { shellState.searchQuery = it },
                onAccountBack = shellState::handleAccountBack,
                onOpenSettings = { shellState.accountRoute = ShellAccountRoute.SETTINGS },
                onNotificationsClick = { },
                onFilterClick = { },
                chips = {
                    when (shellState.selectedTab) {
                        ShellBottomTab.ANIMATION -> AnimationSectionChips(
                            selected = shellState.selectedAnimationSection,
                            onSelect = { shellState.selectedAnimationSection = it },
                            countForSection = animationState::countInSection,
                            modifier = shellCollapsibleChipsModifier(chipsFraction),
                        )
                        ShellBottomTab.READING -> ReadingSectionChips(
                            selected = shellState.selectedReadingSection,
                            onSelect = { shellState.selectedReadingSection = it },
                            countForSection = readingState::countInSection,
                            modifier = shellCollapsibleChipsModifier(chipsFraction),
                        )
                        else -> { }
                    }
                },
            )
        },
        bottomBar = {
            if (!shellState.isAccountDetail) {
                ShellBottomNavBar(
                    selected = shellState.selectedTab,
                    onSelect = shellState::selectTab,
                )
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
        ) {
            when (shellState.selectedTab) {
                ShellBottomTab.HOME -> DiscoveryScreen()
                ShellBottomTab.ANIMATION -> AnimationScreen(
                    stateHolder = animationState,
                    selectedSection = shellState.selectedAnimationSection,
                    scrollBehavior = scrollBehavior,
                )
                ShellBottomTab.READING -> ReadingScreen(
                    stateHolder = readingState,
                    selectedSection = shellState.selectedReadingSection,
                    scrollBehavior = scrollBehavior,
                )
                ShellBottomTab.ACCOUNT -> ShellAccountRouter(
                    route = shellState.accountRoute,
                    profile = shellState.currentProfile,
                    settingsState = settingsState,
                    language = language,
                    isDarkMode = isDarkMode,
                    onLanguageChange = onLanguageChange,
                    onToggleTheme = onToggleTheme,
                    onProfileChange = { shellState.currentProfile = it },
                    onNavigate = { shellState.accountRoute = it },
                    onOpenAnimationList = { shellState.selectTab(ShellBottomTab.ANIMATION) },
                    onOpenReadingList = { shellState.selectTab(ShellBottomTab.READING) },
                    onBackHandlerChange = { shellState.topBarBackHandler = it },
                )
            }
        }
    }
}
