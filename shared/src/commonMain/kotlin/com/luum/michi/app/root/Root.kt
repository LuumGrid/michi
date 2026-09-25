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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.core.navigation.domain.label
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.core.storage.domain.SortPersistence
import com.luum.michi.app.core.storage.domain.SortScope
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.settings.domain.model.ListSort
import com.luum.michi.app.settings.domain.model.toUserSort
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.mediaList.ui.anime.AnimeListScreen
import com.luum.michi.app.mediaList.ui.anime.state.AnimeListStateHolder
import com.luum.michi.app.mediaList.ui.manga.MangaListScreen
import com.luum.michi.app.mediaList.ui.manga.state.MangaListStateHolder
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
    viewer: Viewer? = null,
    onLogout: () -> Unit = {},
    animeListRepository: AnimeListRepository,
    mangaListRepository: MangaListRepository,
    mediaListEntryRepository: MediaListEntryRepository,
    sortPersistence: SortPersistence,
    authServices: List<AuthService>,
    isAuthConfigured: Boolean,
    autoOpenSignIn: Boolean = false,
    onAutoSignInShown: () -> Unit = {},
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
    // The query belongs to the tab where search opened: switching tabs
    // mid-search must not filter the new tab with a foreign query.
    val tabQuery = if (searchTab == tab) activeQuery else ""
    val showScrim = search != null && activeQuery.isEmpty()

    // Synced settings load once per session, on first open — never on
    // composition, never for guests (canSync). The holder dedups repeats.
    LaunchedEffect(state.isSettingsOpen) {
        if (state.isSettingsOpen) settingsState.refresh()
    }

    // Sign-in modal lifecycle: opens once on first anonymous launch,
    // closes on login (viewer appears) wherever it was opened from.
    LaunchedEffect(autoOpenSignIn) {
        if (autoOpenSignIn) {
            state.openSignIn()
            onAutoSignInShown()
        }
    }
    LaunchedEffect(viewer) {
        if (viewer != null) state.closeSignIn()
    }

    // Keyboard follows search: every close path (chevron, scrim, system
    // back, tab switch) funnels through searchActiveTab, so releasing focus
    // here dismisses the keyboard in sync with the toolbar transition.
    // No-op when nothing holds focus.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(state.searchActiveTab) {
        if (state.searchActiveTab == null) {
            focusManager.clearFocus()
        }
    }

    // Anime list holder, recreated per viewer. Loads are explicit and the
    // loader dedups repeats via TTL; guests never load (no user id).
    val listScope = rememberCoroutineScope()
    val viewerId = viewer?.id
    val animeHolder = remember(viewerId, animeListRepository, mediaListEntryRepository, sortPersistence) {
        viewerId?.let {
            AnimeListStateHolder(animeListRepository, mediaListEntryRepository, listScope, sortPersistence)
        }
    }
    val mangaHolder = remember(viewerId, mangaListRepository, mediaListEntryRepository, sortPersistence) {
        viewerId?.let {
            MangaListStateHolder(mangaListRepository, mediaListEntryRepository, listScope, sortPersistence)
        }
    }
    LaunchedEffect(tab, viewerId, settingsState.splitCompletedAnime, settingsState.splitCompletedManga) {
        if (tab == TabSection.ANIME && viewerId != null) {
            animeHolder?.load(viewerId, splitCompleted = settingsState.splitCompletedAnime)
        }
        if (tab == TabSection.MANGA && viewerId != null) {
            mangaHolder?.load(viewerId, splitCompleted = settingsState.splitCompletedManga)
        }
    }

    // Sort memory (Settings persist toggle, per tab) + Default sort:
    // saved persist-sort wins, then the default, then holder defaults.
    // Changing Default sort applies live (effect key); changing it while a
    // saved sort exists leaves that tab on the saved sort. Clearing on
    // toggle-off so stale values never resurrect. Filters never persist.
    val persistSort = settingsState.persistListSort
    val defaultSort = settingsState.listSort
    LaunchedEffect(viewerId, persistSort, defaultSort) {
        if (viewerId == null) return@LaunchedEffect
        if (!persistSort) {
            sortPersistence.clearSort(SortScope.ANIME)
            sortPersistence.clearSort(SortScope.MANGA)
        }
        val animeInitial = resolveInitialSort(
            saved = hydrateSort(sortPersistence, SortScope.ANIME).takeIf { persistSort },
            defaultSort = defaultSort,
        )
        animeHolder?.updateSort(animeInitial.option, animeInitial.order, persist = animeInitial.persist)
        val mangaInitial = resolveInitialSort(
            saved = hydrateSort(sortPersistence, SortScope.MANGA).takeIf { persistSort },
            defaultSort = defaultSort,
        )
        mangaHolder?.updateSort(mangaInitial.option, mangaInitial.order, persist = mangaInitial.persist)
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
                actions = toolbarActions(tab, search != null, state.isSettingsOpen, viewerId == null, strings),
                search = search,
                backContentDescription = strings.backButton,
                clearContentDescription = strings.clearSearchAction,
                onNavigation = { state.applyBackStep() },
                onAction = { id ->
                    when (id) {
                        ACTION_SEARCH -> state.openSearch(tab)
                        ACTION_SETTINGS -> state.openSettings()
                        // List tools only exist on ANIME/MANGA (see
                        // toolbarActions), so these always target a list tab.
                        ACTION_FILTER -> state.openSectionFilter()
                        ACTION_SORT -> state.openListSort()
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
                        viewer = viewer,
                        onLogin = { state.openSignIn() },
                        onLogout = onLogout,
                    )
                } else if (activeTab == TabSection.ANIME && animeHolder != null && viewerId != null) {
                    // Lists own their query: with or without text the screen
                    // renders (full section under the scrim when empty).
                    AnimeListScreen(
                        holder = animeHolder,
                        selected = state.selectedAnimeSection,
                        onSelectSection = { state.selectedAnimeSection = it },
                        onRetry = {
                            animeHolder.load(
                                viewerId,
                                forceRefresh = true,
                                splitCompleted = settingsState.splitCompletedAnime,
                            )
                        },
                        bottomPadding = padding.calculateBottomPadding(),
                        isRefreshing = animeHolder.isRefreshing,
                        onRefresh = {
                            animeHolder.load(
                                viewerId,
                                forceRefresh = true,
                                splitCompleted = settingsState.splitCompletedAnime,
                            )
                        },
                        query = tabQuery,
                        isFilterOpen = state.isSectionFilterOpen,
                        onDismissFilter = { state.closeSectionFilter() },
                        isSortOpen = state.isListSortOpen,
                        onDismissSort = { state.closeListSort() },
                        sortPersist = persistSort,
                        splitCompleted = settingsState.splitCompletedAnime,
                        hideAdult = !settingsState.displayAdultContent,
                        onOpenDetail = { state.openMedia(it) },
                        onEditEntry = { state.openEditor(it) },
                    )
                } else if (activeTab == TabSection.MANGA && mangaHolder != null && viewerId != null) {
                    MangaListScreen(
                        holder = mangaHolder,
                        selected = state.selectedMangaSection,
                        onSelectSection = { state.selectedMangaSection = it },
                        onRetry = {
                            mangaHolder.load(
                                viewerId,
                                forceRefresh = true,
                                splitCompleted = settingsState.splitCompletedManga,
                            )
                        },
                        bottomPadding = padding.calculateBottomPadding(),
                        isRefreshing = mangaHolder.isRefreshing,
                        onRefresh = {
                            mangaHolder.load(
                                viewerId,
                                forceRefresh = true,
                                splitCompleted = settingsState.splitCompletedManga,
                            )
                        },
                        query = tabQuery,
                        isFilterOpen = state.isSectionFilterOpen,
                        onDismissFilter = { state.closeSectionFilter() },
                        isSortOpen = state.isListSortOpen,
                        onDismissSort = { state.closeListSort() },
                        sortPersist = persistSort,
                        splitCompleted = settingsState.splitCompletedManga,
                        hideAdult = !settingsState.displayAdultContent,
                        onOpenDetail = { state.openMedia(it) },
                        onEditEntry = { state.openEditor(it) },
                    )
                } else if (search != null && activeQuery.isNotEmpty()) {
                    // DISCOVER (and guests): server search lands in a later step.
                    MessagePanel(
                        title = strings.searchNoResultsLabel,
                        message = null,
                        icon = AppIcons.Search,
                        actionLabel = null,
                        onAction = {},
                    )
                } else if (
                    (activeTab == TabSection.ANIME || activeTab == TabSection.MANGA ||
                        activeTab == TabSection.ACCOUNT) && viewerId == null
                ) {
                    // Gated tabs without a session: sign-in prompt opening
                    // the modal (the only login surface besides first launch).
                    val tabItem = tabs(strings).first { it.section == activeTab }
                    MessagePanel(
                        title = activeTab.label(strings),
                        message = strings.authGuestPrompt,
                        icon = tabItem.icon,
                        actionLabel = strings.settingsSignInTitle,
                        onAction = { state.openSignIn() },
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
            if (state.isSignInOpen) {
                AuthModal(
                    services = authServices,
                    isConfigured = isAuthConfigured,
                    configMissingLabel = strings.authConfigurationMissing,
                    language = language,
                    onLanguageChange = onLanguageChange,
                    guestLabel = strings.authContinueAsGuestAction,
                    onDismiss = { state.closeSignIn() },
                )
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

/** Sort memory restore: unknown/corrupt names fall back to null (holder
 * defaults), never crash. Pure for tests. */
internal fun hydrateSort(
    persistence: SortPersistence,
    scope: SortScope,
): Pair<UserListSort, UserListOrder>? {
    val (sort, order) = persistence.loadSort(scope) ?: return null
    val option = UserListSort.entries.firstOrNull { it.name == sort } ?: return null
    val direction = UserListOrder.entries.firstOrNull { it.name == order } ?: return null
    return option to direction
}

/** Session-start sort per tab. Pure for tests. */
internal data class InitialSort(
    val option: UserListSort,
    val order: UserListOrder,
    /** True only when restoring the user's own saved sort. */
    val persist: Boolean,
)

/**
 * Precedence: saved persist-sort wins, then the Settings default sort, then
 * holder defaults (handled by the caller when this returns the default —
 * persist is false so defaults are never written back as user choices).
 * Explicit always beats default: changing Default sort while a saved sort
 * exists leaves the lists on the saved sort.
 */
internal fun resolveInitialSort(
    saved: Pair<UserListSort, UserListOrder>?,
    defaultSort: ListSort,
): InitialSort {
    if (saved != null) {
        return InitialSort(saved.first, saved.second, persist = true)
    }
    val (option, order) = defaultSort.toUserSort()
    return InitialSort(option, order, persist = false)
}

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
    isGuest: Boolean,
    strings: LanguageStrings,
): List<ToolbarAction> {
    if (isSearching || isSettingsOpen) return emptyList()
    // Gated tabs without a session keep the calendar only (public airing
    // data, no login needed): filter/sort/search operate on lists that
    // don't exist. Calendar stays visible-but-inert until its surface
    // lands — same as for logged users today.
    val isGatedGuest = isGuest && (tab == TabSection.ANIME || tab == TabSection.MANGA)
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
        if (!isGatedGuest && (tab == TabSection.ANIME || tab == TabSection.MANGA)) {
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
            if (!isGatedGuest) {
                add(
                    ToolbarAction(
                        id = ACTION_SEARCH,
                        icon = AppIcons.Search,
                        contentDescription = strings.searchTitle,
                    ),
                )
            }
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
