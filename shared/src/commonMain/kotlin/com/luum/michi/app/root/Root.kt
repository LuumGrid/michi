package com.luum.michi.app.root

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.ui.CalendarScreen
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.medialist.domain.SortPersistence
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.core.platform.domain.ShareLauncher
import com.luum.michi.app.core.platform.domain.UrlOpener
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.discover.domain.DashboardFeedCache
import com.luum.michi.app.discover.domain.DashboardRepository
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.root.state.State
import com.luum.michi.app.root.state.detailSurfaceKey
import com.luum.michi.app.root.state.rememberState
import com.luum.michi.app.root.state.resolveStartDestination
import com.luum.michi.app.root.state.startDestinationChoice
import com.luum.michi.app.settings.ui.SettingsScreen
import com.luum.michi.app.settings.ui.state.SettingsState
import com.luum.michi.app.ui.components.SearchScrim
import com.luum.michi.app.ui.components.ToolbarSearch
import com.luum.michi.app.ui.components.tabFadeSpec
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
    settingsState: SettingsState,
    viewer: Viewer? = null,
    providedState: State? = null,
    strings: LanguageStrings = Strings.current,
    onLogout: () -> Unit = {},
    animeListRepository: AnimeListRepository,
    mangaListRepository: MangaListRepository,
    mediaListEntryRepository: MediaListEntryRepository,
    mediaDetailRepository: MediaDetailRepository,
    characterDetailRepository: CharacterDetailRepository,
    staffDetailRepository: StaffDetailRepository,
    studioDetailRepository: StudioDetailRepository,
    exploreRepository: ExploreRepository,
    calendarRepository: CalendarRepository,
    dashboardRepository: DashboardRepository,
    dashboardFeedCache: DashboardFeedCache,
    urlOpener: UrlOpener,
    shareLauncher: ShareLauncher,
    sortPersistence: SortPersistence,
    authServices: List<AuthService>,
    isAuthConfigured: Boolean,
    autoOpenSignIn: Boolean = false,
    onAutoSignInShown: () -> Unit = {},
    deepLinkMediaId: Int? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    // The "Pestaña al abrir" setting seeds the shell state once (startup only):
    // a later settings change never moves the user. A guest can only land on
    // DISCOVER (the other tabs need a session) and a section id that no longer
    // exists (split-completed off) falls back to ALL. Resolved once here (not
    // in a default argument), so Root's recompositions never re-read the six
    // settings.
    val state = providedState ?: rememberState(
        startDestination = remember {
            resolveStartDestination(
                startTab = settingsState.startTab,
                animeSectionId = settingsState.startAnimeSectionId,
                mangaSectionId = settingsState.startMangaSectionId,
                splitCompletedAnime = settingsState.splitCompletedAnime,
                splitCompletedManga = settingsState.splitCompletedManga,
                isGuest = viewer == null,
            )
        },
    )
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

    val viewerId = viewer?.id

    // "Pestaña al abrir": the settings row stays feature-agnostic, so Root
    // resolves the localized sub-choice (settings never imports a feature) and
    // the section ids are validated against the live lists.
    val startChoice = startDestinationChoice(
        tab = settingsState.startTab,
        catalogType = settingsState.startCatalogType,
        animeSectionId = settingsState.startAnimeSectionId,
        mangaSectionId = settingsState.startMangaSectionId,
        splitCompletedAnime = settingsState.splitCompletedAnime,
        splitCompletedManga = settingsState.splitCompletedManga,
        strings = strings,
    )

    // Per-surface saveable state (scroll positions): Root owns the holder so a
    // tab, each detail entry and the overlays keep their own position. A popped
    // detail entry drops its state (a new entry always starts at the top).
    val surfaceStates = rememberSaveableStateHolder()

    val holders = rememberRootHolders(
        viewerId = viewerId,
        settingsState = settingsState,
        strings = strings,
        animeListRepository = animeListRepository,
        mangaListRepository = mangaListRepository,
        mediaListEntryRepository = mediaListEntryRepository,
        mediaDetailRepository = mediaDetailRepository,
        characterDetailRepository = characterDetailRepository,
        staffDetailRepository = staffDetailRepository,
        studioDetailRepository = studioDetailRepository,
        exploreRepository = exploreRepository,
        calendarRepository = calendarRepository,
        dashboardRepository = dashboardRepository,
        dashboardFeedCache = dashboardFeedCache,
        sortPersistence = sortPersistence,
    )

    RootEffects(
        state = state,
        holders = holders,
        settingsState = settingsState,
        viewer = viewer,
        sortPersistence = sortPersistence,
        surfaceStates = surfaceStates,
        autoOpenSignIn = autoOpenSignIn,
        onAutoSignInShown = onAutoSignInShown,
        deepLinkMediaId = deepLinkMediaId,
        onDeepLinkHandled = onDeepLinkHandled,
    )

    Scaffold(
        topBar = {
            RootToolbar(
                state = state,
                tab = tab,
                search = search,
                searchTab = searchTab,
                holders = holders,
                isGuest = viewerId == null,
                strings = strings,
            )
        },
        bottomBar = {
            RootTabBar(tab = tab, state = state, strings = strings)
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
                // One saved surface per view: switching surfaces (tab ↔ detail,
                // detail ↔ detail, overlay) preserves each one's saveable state
                // (scroll) without recreating the holders above. The slot's target
                // tab is part of the key: during a tab transition both slots compose
                // and equal keys would crash.
                val surface = surfaceKey(activeTab, state)
                surfaceStates.SaveableStateProvider(surface) {
                    key(surface) {
                        if (state.isSettingsOpen) {
                            SettingsScreen(
                                settingsState = settingsState,
                                startDestination = startChoice,
                                viewer = viewer,
                                onLogin = { state.openSignIn() },
                                onLogout = onLogout,
                                bottomPadding = padding.calculateBottomPadding(),
                            )
                        } else if (state.isDetailOpen) {
                            // Detail page over the current tab (tab bar hidden):
                            // pushes onto detailStack, back pops one at a time.
                            RootDetailHost(
                                destination = state.currentDetail,
                                holders = holders,
                                state = state,
                                strings = strings,
                                bottomPadding = padding.calculateBottomPadding(),
                                urlOpener = urlOpener,
                            )
                        } else if (state.isCalendarOpen) {
                            // Airing schedule overlay (fullscreen, tab bar hidden):
                            // public feed, so guests render the same surface.
                            CalendarScreen(
                                holder = holders.calendar,
                                onRetry = { holders.calendar.reloadSelected() },
                                bottomPadding = padding.calculateBottomPadding(),
                                isRefreshing = holders.calendar.isLoading,
                                onRefresh = { holders.calendar.reloadSelected() },
                                isFilterOpen = state.isCalendarFilterOpen,
                                onDismissFilter = { state.closeCalendarFilter() },
                                onOpenDetail = { state.openMedia(it) },
                                onOpenUrl = { urlOpener.open(it) },
                            )
                        } else {
                            RootTabContent(
                                activeTab = activeTab,
                                state = state,
                                holders = holders,
                                settingsState = settingsState,
                                viewerId = viewerId,
                                isSearching = search != null,
                                activeQuery = activeQuery,
                                tabQuery = tabQuery,
                                strings = strings,
                                bottomPadding = padding.calculateBottomPadding(),
                            )
                        }
                    }
                }
            }
            if (showScrim) {
                SearchScrim(onDismiss = { state.applyBackStep() })
            }
            RootModals(
                state = state,
                holders = holders,
                viewerId = viewerId,
                settingsState = settingsState,
                authServices = authServices,
                isAuthConfigured = isAuthConfigured,
                mediaListEntryRepository = mediaListEntryRepository,
                mediaDetailRepository = mediaDetailRepository,
                urlOpener = urlOpener,
                shareLauncher = shareLauncher,
                strings = strings,
            )
        }
    }
}

/** Which saved surface the content area shows: the slot's tab + the view. */
private fun surfaceKey(tab: TabSection, state: State): String = when {
    state.isSettingsOpen -> "${tab.name}:settings"
    state.isDetailOpen -> detailSurfaceKey(tab, state.currentDetailEntryId)
    state.isCalendarOpen -> "${tab.name}:calendar"
    state.isExploreOpen -> "${tab.name}:explore"
    state.isNotificationsOpen -> "${tab.name}:notifications"
    else -> "${tab.name}:tab"
}
