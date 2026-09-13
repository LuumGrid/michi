package com.luum.michi.app.root

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.core.navigation.domain.label
import com.luum.michi.app.root.state.AccountRoute
import com.luum.michi.app.root.state.State
import com.luum.michi.app.root.state.rememberState
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.SearchScrim
import com.luum.michi.app.ui.components.TabBar
import com.luum.michi.app.ui.components.TabItem
import com.luum.michi.app.ui.components.Toolbar
import com.luum.michi.app.ui.components.ToolbarAction
import com.luum.michi.app.ui.components.ToolbarNavigation
import com.luum.michi.app.ui.components.ToolbarSearch
import com.luum.michi.app.ui.components.tabFadeSpec
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings
import com.luum.michi.app.ui.language.searchHintFor

/**
 * Slim app orchestrator: owns the [State], derives the topbar per tab and
 * routes the content area. Cross-feature coordination lives here — features
 * never import each other or this package.
 *
 * System-back wiring happens at the platform entries (MainActivity /
 * MainViewController) via [State.applyBackStep]; every in-app affordance
 * (toolbar back, scrim tap) already routes through it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Root(
    state: State = rememberState(),
    strings: LanguageStrings = Strings.current,
) {
    val tab = state.selectedTab
    val searchTab = state.searchActiveTab
    val search = searchTab?.let {
        ToolbarSearch(
            query = state.searchQuery(it),
            hint = searchHintFor(it, strings).orEmpty(),
            autoFocus = true,
        )
    }
    // Derived, no state of its own: scrim while expanded with an empty query,
    // replaced by the result list on the first letter.
    val activeQuery = searchTab?.let { state.searchQuery(it) }.orEmpty()
    val showScrim = search != null && activeQuery.isEmpty()

    // Enter-always: the floating toolbar hides on scroll down, returns on scroll up.
    val toolbarScroll = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(toolbarScroll.nestedScrollConnection),
        topBar = {
            Toolbar(
                title = toolbarTitle(state, tab, strings),
                navigation = if (search != null || state.isDetailOpen || state.isAccountDetail) {
                    ToolbarNavigation.Back(strings.backButton)
                } else {
                    ToolbarNavigation.None
                },
                actions = toolbarActions(tab, search != null, strings),
                search = search,
                backContentDescription = strings.backButton,
                clearContentDescription = null,
                onNavigation = { state.applyBackStep() },
                onAction = { id ->
                    when (id) {
                        ACTION_SEARCH -> state.openSearch(tab)
                        ACTION_SETTINGS -> state.openAccountSettings()
                    }
                },
                onSearchChange = { query ->
                    searchTab?.let { state.updateSearch(it, query) }
                },
                onSearchSubmit = {},
                onSearchClose = { state.applyBackStep() },
                onSearchClear = {
                    searchTab?.let { state.clearSearch(it) }
                },
                scrollBehavior = toolbarScroll,
            )
        },
        bottomBar = {
            TabBar(
                selected = tab,
                tabs = tabs(strings),
                onSelect = { state.selectTab(it) },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Top only: content scrolls behind the floating tab bar.
                .padding(top = padding.calculateTopPadding()),
        ) {
            // Same fade as the tab bubble (see TabMotion.kt): tab state drives
            // the transition, never data loads — real screens stream in after.
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(animationSpec = tabFadeSpec()) togetherWith
                        fadeOut(animationSpec = tabFadeSpec())
                },
                label = "tab-content",
            ) { activeTab ->
            if (search != null && activeQuery.isNotEmpty()) {
                MessagePanel(
                    title = strings.searchNoResultsLabel,
                    message = null,
                    icon = AppIcons.Search,
                    actionLabel = null,
                    onAction = {},
                )
            } else {
                val tabItem = tabs(strings).first { it.section == activeTab }
                MessagePanel(
                    title = activeTab.label(strings),
                    message = null,
                    icon = tabItem.icon,
                    actionLabel = null,
                    onAction = {},
                )
            }
            }
            if (showScrim) {
                SearchScrim(onDismiss = { state.applyBackStep() })
            }
        }
    }
}

/** The 4 top-level destinations. Root chooses the icons; ui stays dumb. */
internal fun tabs(strings: LanguageStrings): List<TabItem> = listOf(
    TabItem(
        section = TabSection.DISCOVER,
        label = TabSection.DISCOVER.label(strings),
        icon = Icons.Filled.Home,
    ),
    TabItem(
        section = TabSection.ANIME,
        label = TabSection.ANIME.label(strings),
        icon = Icons.Filled.PlayArrow,
    ),
    TabItem(
        section = TabSection.MANGA,
        label = TabSection.MANGA.label(strings),
        icon = Icons.AutoMirrored.Filled.List,
    ),
    TabItem(
        section = TabSection.ACCOUNT,
        label = TabSection.ACCOUNT.label(strings),
        icon = Icons.Filled.Person,
    ),
)

private const val ACTION_SEARCH = "search"
private const val ACTION_SETTINGS = "settings"

private fun toolbarActions(
    tab: TabSection,
    isSearching: Boolean,
    strings: LanguageStrings,
): List<ToolbarAction> {
    if (isSearching) return emptyList()
    return buildList {
        if (tab != TabSection.ACCOUNT) {
            add(
                ToolbarAction(
                    id = ACTION_SEARCH,
                    icon = AppIcons.Search,
                    contentDescription = strings.searchTitle,
                ),
            )
        } else {
            add(
                ToolbarAction(
                    id = ACTION_SETTINGS,
                    icon = Icons.Filled.Settings,
                    contentDescription = strings.settingsAction,
                ),
            )
        }
    }
}

private fun toolbarTitle(
    state: State,
    tab: TabSection,
    strings: LanguageStrings,
): String = when {
    state.isAccountDetail && state.accountRoute == AccountRoute.SETTINGS -> strings.accountSettingsTitle
    state.isAccountDetail && state.accountRoute == AccountRoute.STATS -> strings.accountStatsTitle
    else -> tab.label(strings)
}
