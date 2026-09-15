package com.luum.michi.app.root

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.core.navigation.domain.label
import com.luum.michi.app.root.state.State
import com.luum.michi.app.root.state.rememberState
import com.luum.michi.app.settings.ui.SettingsScreen
import com.luum.michi.app.settings.ui.state.SettingsState
import com.luum.michi.app.ui.components.BottomEdgeFade
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.SearchScrim
import com.luum.michi.app.ui.components.TabBar
import com.luum.michi.app.ui.components.TabItem
import com.luum.michi.app.ui.components.TopEdgeFade
import com.luum.michi.app.ui.components.Toolbar
import com.luum.michi.app.ui.components.ToolbarAction
import com.luum.michi.app.ui.components.ToolbarNavigation
import com.luum.michi.app.ui.components.ToolbarSearch
import com.luum.michi.app.ui.components.tabFadeSpec
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.Strings
import com.luum.michi.app.ui.language.searchHintFor
import com.luum.michi.app.ui.theme.AppFont
import com.luum.michi.app.ui.theme.ThemeColors
import com.luum.michi.app.ui.theme.ThemeType
import kotlin.math.roundToInt

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
    settingsState: SettingsState,
    state: State = rememberState(),
    strings: LanguageStrings = Strings.current,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    palette: ThemeColors,
    onPaletteChange: (ThemeColors) -> Unit,
    themeType: ThemeType,
    onThemeTypeChange: (ThemeType) -> Unit,
    font: AppFont,
    onFontChange: (AppFont) -> Unit,
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

    // Synced settings load once per session, on first open — never on
    // composition, never for guests (canSync). The holder dedups repeats.
    LaunchedEffect(state.isSettingsOpen) {
        if (state.isSettingsOpen) settingsState.refresh()
    }

    // Pinned by default: the toolbar only hides on scroll for surfaces that
    // explicitly opt in via [hidesToolbarOnScroll]. The veil rides the same
    // offset so it stays glued to the toolbar when collapsing.
    val toolbarScroll = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val hideToolbarOnScroll = hidesToolbarOnScroll(tab, state)
    val veilOffsetPx = if (hideToolbarOnScroll) {
        toolbarScroll.state.heightOffset.roundToInt()
    } else {
        0
    }

    Scaffold(
        modifier = if (hideToolbarOnScroll) {
            Modifier.nestedScroll(toolbarScroll.nestedScrollConnection)
        } else {
            Modifier
        },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(x = 0, y = veilOffsetPx) },
            ) {
                TopEdgeFade(modifier = Modifier.matchParentSize())
                Toolbar(
                title = toolbarTitle(state, tab, strings),
                navigation = if (search != null || state.isDetailOpen || state.isSettingsOpen) {
                    ToolbarNavigation.Back(strings.backButton)
                } else {
                    ToolbarNavigation.None
                },
                actions = toolbarActions(tab, search != null, state.isSettingsOpen, strings),
                search = search,
                backContentDescription = strings.backButton,
                clearContentDescription = strings.clearSearchAction,
                onNavigation = { state.applyBackStep() },
                onAction = { id ->
                    when (id) {
                        ACTION_SEARCH -> state.openSearch(tab)
                        ACTION_SETTINGS -> state.openSettings()
                        // TODO: wire to the list filter/sort sheets when the
                        // anime/manga list screens land. Visible but inert today.
                        ACTION_FILTER,
                        ACTION_SORT,
                        // TODO: wire to the schedule surface when it lands.
                        ACTION_CALENDAR,
                        -> Unit
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
                scrollBehavior = toolbarScroll.takeIf { hideToolbarOnScroll },
            )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showsTabBar(tab, state),
                enter = slideInVertically(animationSpec = tabFadeSpec()) { it } +
                    fadeIn(animationSpec = tabFadeSpec()),
                exit = slideOutVertically(animationSpec = tabFadeSpec()) { it } +
                    fadeOut(animationSpec = tabFadeSpec()),
                label = "tab-bar-visibility",
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    BottomEdgeFade(modifier = Modifier.matchParentSize())
                    TabBar(
                    selected = tab,
                    tabs = tabs(strings),
                    onSelect = { state.selectTab(it) },
                )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Top only: content scrolls behind the floating tab bar.
                .padding(top = padding.calculateTopPadding()),
        ) {
            // Swipe-like directional swap: sign of the tab order only, fixed
            // magnitude — direct initial -> target, never through middle tabs.
            // Tab state drives the transition, never data loads.
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val forward = TabSection.entries.indexOf(targetState) >
                        TabSection.entries.indexOf(initialState)
                    val enterFrom = if (forward) 1 else -1
                    (fadeIn(animationSpec = tabFadeSpec()) +
                        slideInHorizontally(animationSpec = tabFadeSpec()) { it / 4 * enterFrom } togetherWith
                        fadeOut(animationSpec = tabFadeSpec()) +
                        slideOutHorizontally(animationSpec = tabFadeSpec()) { -it / 4 * enterFrom }) using
                        SizeTransform(clip = false)
                },
                label = "tab-content",
            ) { activeTab ->
                if (state.isSettingsOpen) {
                    SettingsScreen(
                        settingsState = settingsState,
                        language = language,
                        onLanguageChange = onLanguageChange,
                        palette = palette,
                        onPaletteChange = onPaletteChange,
                        themeType = themeType,
                        onThemeTypeChange = onThemeTypeChange,
                        font = font,
                        onFontChange = onFontChange,
                    )
                } else if (search != null && activeQuery.isNotEmpty()) {
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
@Composable
internal fun tabs(strings: LanguageStrings): List<TabItem> = listOf(
    TabItem(
        section = TabSection.DISCOVER,
        label = TabSection.DISCOVER.label(strings),
        icon = AppIcons.Discover,
    ),
    TabItem(
        section = TabSection.ANIME,
        label = TabSection.ANIME.label(strings),
        icon = AppIcons.Anime,
    ),
    TabItem(
        section = TabSection.MANGA,
        label = TabSection.MANGA.label(strings),
        icon = AppIcons.Manga,
    ),
    TabItem(
        section = TabSection.ACCOUNT,
        label = TabSection.ACCOUNT.label(strings),
        icon = AppIcons.Account,
    ),
)

private const val ACTION_SEARCH = "search"
private const val ACTION_SETTINGS = "settings"
private const val ACTION_FILTER = "filter"
private const val ACTION_SORT = "sort"
private const val ACTION_CALENDAR = "calendar"

/** Shared capsule group for the list tools (reference: one capsule, lone search). */
private const val GROUP_LIST_TOOLS = "list-tools"

@Composable
private fun toolbarActions(
    tab: TabSection,
    isSearching: Boolean,
    isSettingsOpen: Boolean,
    strings: LanguageStrings,
): List<ToolbarAction> {
    if (isSearching || isSettingsOpen) return emptyList()
    return buildList {
        if (tab != TabSection.ACCOUNT) {
            // Ungrouped on purpose: lone circle left of the tools capsule.
            add(
                ToolbarAction(
                    id = ACTION_CALENDAR,
                    icon = AppIcons.Calendar,
                    contentDescription = strings.calendarAction,
                ),
            )
        }
        if (tab == TabSection.ANIME || tab == TabSection.MANGA) {
            add(
                ToolbarAction(
                    id = ACTION_FILTER,
                    icon = AppIcons.Filter,
                    contentDescription = strings.filterAction,
                    group = GROUP_LIST_TOOLS,
                ),
            )
            add(
                ToolbarAction(
                    id = ACTION_SORT,
                    icon = AppIcons.Sort,
                    contentDescription = strings.sortAction,
                    group = GROUP_LIST_TOOLS,
                ),
            )
        }
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
                    icon = AppIcons.Settings,
                    contentDescription = strings.settingsAction,
                ),
            )
        }
    }
}

/**
 * Explicit opt-in per surface: the toolbar stays pinned unless a surface
 * asks to hide it on scroll (like action groups: explicit, never by
 * omission). No surface opts in yet; the first long list will add its case.
 */
private fun hidesToolbarOnScroll(tab: TabSection, state: State): Boolean = false

/**
 * Explicit opt-in per surface: the tab bar shows unless a surface asks to
 * hide it (full-screen surfaces like Settings). Default is visible.
 */
private fun showsTabBar(tab: TabSection, state: State): Boolean = !state.isSettingsOpen

private fun toolbarTitle(
    state: State,
    tab: TabSection,
    strings: LanguageStrings,
): String = if (state.isSettingsOpen) {
    strings.settingsTitle
} else {
    tab.label(strings)
}
