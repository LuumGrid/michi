package com.luum.michi.app.shell

import androidx.compose.foundation.background
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.account.data.AccountRepository
import com.luum.michi.app.account.presentation.state.rememberAccountStateHolder
import com.luum.michi.app.animation.data.AnimationListRepository
import com.luum.michi.app.animation.presentation.AnimationScreen
import com.luum.michi.app.animation.presentation.components.AnimationSectionChips
import com.luum.michi.app.animation.presentation.state.rememberAnimationListStateHolder
import com.luum.michi.app.explore.data.ExploreRepository
import com.luum.michi.app.explore.presentation.ExploreScreen
import com.luum.michi.app.explore.presentation.state.rememberExploreStateHolder
import com.luum.michi.app.calendar.data.CalendarRepository
import com.luum.michi.app.calendar.presentation.CalendarScreen
import com.luum.michi.app.calendar.presentation.state.rememberCalendarStateHolder
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformSystemBackHandler
import com.luum.michi.app.core.session.Viewer
import com.luum.michi.app.dashboard.data.DashboardRepository
import com.luum.michi.app.dashboard.presentation.DashboardScreen
import com.luum.michi.app.dashboard.presentation.state.rememberDashboardStateHolder
import com.luum.michi.app.mediaDetail.data.MediaDetailRepository
import com.luum.michi.app.mediaDetail.data.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.presentation.MediaDetailScreen
import com.luum.michi.app.mediaDetail.presentation.components.MediaDetailEditorSheet
import com.luum.michi.app.mediaDetail.presentation.state.rememberMediaDetailStateHolder
import com.luum.michi.app.mediaDetail.presentation.state.rememberMediaEntryEditorState
import com.luum.michi.app.feed.data.FeedRepository
import com.luum.michi.app.feed.presentation.FeedScreen
import com.luum.michi.app.feed.presentation.state.rememberFeedStateHolder
import com.luum.michi.app.reading.data.ReadingListRepository
import com.luum.michi.app.reading.presentation.ReadingScreen
import com.luum.michi.app.reading.presentation.components.ReadingSectionChips
import com.luum.michi.app.reading.presentation.state.rememberReadingListStateHolder
import com.luum.michi.app.search.data.SearchRepository
import com.luum.michi.app.search.presentation.SearchScreen
import com.luum.michi.app.search.presentation.state.rememberSearchStateHolder
import com.luum.michi.app.settings.presentation.state.rememberSettingsState
import com.luum.michi.app.shell.components.ShellAccountRouter
import com.luum.michi.app.core.platform.rememberPlatformFilterSettings
import com.luum.michi.app.core.platform.components.PlatformListFilterSheet
import com.luum.michi.app.core.platform.model.UserListSort
import com.luum.michi.app.core.platform.model.UserListOrder
import com.luum.michi.app.feed.presentation.components.FeedFilterSheet
import com.luum.michi.app.shell.components.ShellBottomNavBar
import com.luum.michi.app.shell.components.ShellBottomTab
import com.luum.michi.app.shell.components.ShellTopBar
import com.luum.michi.app.shell.components.label
import com.luum.michi.app.shell.components.shellCollapsibleChipsModifier
import com.luum.michi.app.shell.state.ShellAccountRoute
import com.luum.michi.app.shell.state.rememberShellState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShellScreen(
    viewer: Viewer,
    animationListRepository: AnimationListRepository,
    readingListRepository: ReadingListRepository,
    accountRepository: AccountRepository,
    dashboardRepository: DashboardRepository,
    exploreRepository: ExploreRepository,
    calendarRepository: CalendarRepository,
    mediaDetailRepository: MediaDetailRepository,
    mediaListEntryRepository: MediaListEntryRepository,
    searchRepository: SearchRepository,
    feedRepository: FeedRepository,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
) {
    val strings = LanguageProvider.strings
    val shellState = rememberShellState(viewer)
    val animationState = rememberAnimationListStateHolder(animationListRepository, mediaListEntryRepository, viewer.id)
    val readingState = rememberReadingListStateHolder(readingListRepository, mediaListEntryRepository, viewer.id)
    val accountState = rememberAccountStateHolder(
        repository = accountRepository,
        viewerId = viewer.id,
    )
    val dashboardState = rememberDashboardStateHolder(dashboardRepository)
    val exploreState = rememberExploreStateHolder(exploreRepository)
    val calendarState = rememberCalendarStateHolder(calendarRepository)
    val mediaDetailState = rememberMediaDetailStateHolder(mediaDetailRepository, viewerId = viewer.id)
    val searchState = rememberSearchStateHolder(searchRepository)
    val feedState = rememberFeedStateHolder(feedRepository, viewer.id)
    val settingsState = rememberSettingsState()
    var showExploreFilters by remember { mutableStateOf(false) }
    var showListFilterSheet by remember { mutableStateOf(false) }
    var showFeedFilterSheet by remember { mutableStateOf(false) }

    val filterSettings = rememberPlatformFilterSettings()
    LaunchedEffect(Unit) {
        filterSettings.loadFilter()?.let { (sortName, orderName, persist) ->
            val sort = UserListSort.values().firstOrNull { it.name == sortName } ?: UserListSort.FOLLOW_LIST
            val order = UserListOrder.values().firstOrNull { it.name == orderName } ?: UserListOrder.DESCENDING
            animationState.updateSort(sort, order, persist)
            readingState.updateSort(sort, order, persist)
        }
    }

    val tabStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(shellState.selectedTab) {
        when (shellState.selectedTab) {
            ShellBottomTab.ANIMATION -> {
                if (animationState.entries.isEmpty() && !animationState.isLoading) {
                    animationState.load(viewer.id)
                }
            }
            ShellBottomTab.READING -> {
                if (readingState.entries.isEmpty() && !readingState.isLoading) {
                    readingState.load(viewer.id)
                }
            }
            ShellBottomTab.ACCOUNT -> {
                if (accountState.stats.animeCount == 0 && !accountState.isLoading) {
                    accountState.load(viewer.id)
                }
            }
            ShellBottomTab.FEED -> {
                if (feedState.activities.isEmpty() && !feedState.isLoading) {
                    feedState.load()
                }
            }
            else -> {}
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val chipsFraction by remember {
        derivedStateOf {
            val limit = scrollBehavior.state.heightOffsetLimit
            if (limit < 0f) (scrollBehavior.state.heightOffset / limit).coerceIn(0f, 1f) else 0f
        }
    }

    PlatformSystemBackHandler(
        enabled = shellState.isEditorOpen,
        onBack = shellState::closeEditor,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && shellState.isMediaDetailOpen,
        onBack = shellState::closeMedia,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isMediaDetailOpen && shellState.isExploreOpen,
        onBack = {
            if (showExploreFilters) {
                showExploreFilters = false
            } else {
                shellState.closeExplore()
            }
        },
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isMediaDetailOpen && !shellState.isExploreOpen &&
            shellState.isCalendarOpen,
        onBack = shellState::closeCalendar,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isMediaDetailOpen && !shellState.isExploreOpen &&
            !shellState.isCalendarOpen && shellState.isAccountDetail,
        onBack = shellState::handleAccountBack,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isMediaDetailOpen && !shellState.isExploreOpen &&
            !shellState.isCalendarOpen && shellState.isSearchTab && shellState.isSearchActive,
        onBack = shellState::closeSearch,
    )

    val titleText = when {
        shellState.isMediaDetailOpen -> strings.mediaDetailTitle
        shellState.isExploreOpen -> strings.exploreTitle
        shellState.isCalendarOpen -> strings.calendarTitle
        shellState.selectedTab == ShellBottomTab.ACCOUNT &&
            shellState.accountRoute == ShellAccountRoute.SETTINGS -> strings.settingsAction
        shellState.selectedTab == ShellBottomTab.ACCOUNT &&
            shellState.accountRoute == ShellAccountRoute.EDIT_PROFILE -> strings.accountEditProfileAction
        shellState.selectedTab == ShellBottomTab.ACCOUNT &&
            shellState.accountRoute == ShellAccountRoute.SHARE_PROFILE -> strings.accountShareProfileAction
        shellState.selectedTab == ShellBottomTab.ACCOUNT ->
            shellState.currentProfile.username
        else -> shellState.selectedTab.label(strings)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ShellTopBar(
                selectedTab = shellState.selectedTab,
                isAccountDetail = shellState.isAccountDetail,
                isMediaDetailOpen = shellState.isMediaDetailOpen,
                isExploreOpen = shellState.isExploreOpen,
                isCalendarOpen = shellState.isCalendarOpen,
                isSearchActive = shellState.isSearchActive,
                isSearchTab = shellState.isSearchTab,
                searchQuery = shellState.searchQuery,
                titleText = titleText,
                scrollBehavior = scrollBehavior,
                onOpenSearch = shellState::openSearch,
                onCloseSearch = shellState::closeSearch,
                onSearchQueryChange = { shellState.searchQuery = it },
                onAccountBack = shellState::handleAccountBack,
                onMediaBack = shellState::closeMedia,
                onExploreBack = {
                    if (showExploreFilters) {
                        showExploreFilters = false
                    } else {
                        shellState.closeExplore()
                    }
                },
                onCalendarBack = shellState::closeCalendar,
                onOpenSettings = { shellState.accountRoute = ShellAccountRoute.SETTINGS },
                onNotificationsClick = { },
                onFilterClick = {
                    if (shellState.selectedTab == ShellBottomTab.FEED) {
                        showFeedFilterSheet = true
                    } else {
                        showListFilterSheet = true
                    }
                },
                onForumClick = { },
                exploreQuery = exploreState.query,
                onExploreQueryChange = { exploreState.updateFilters(newQuery = it) },
                showExploreFiltersToggle = !exploreState.isEntitySearch(),
                isExploreFiltersOpen = showExploreFilters,
                onToggleExploreFilters = { showExploreFilters = !showExploreFilters },
                chips = {
                    if (!shellState.isSearchActive) {
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
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                ),
        ) {
            if (shellState.isSearchActive && shellState.selectedTab == ShellBottomTab.HOME) {
                SearchScreen(
                    query = shellState.searchQuery,
                    stateHolder = searchState,
                    onOpenMedia = shellState::openMedia,
                    onEditMedia = shellState::openEditor,
                )
            } else {
                Crossfade(
                    targetState = shellState.selectedTab,
                    animationSpec = tween(durationMillis = 220),
                    modifier = Modifier.fillMaxSize(),
                    label = "tabCrossfade",
                ) { tab ->
                    tabStateHolder.SaveableStateProvider(tab) {
                        when (tab) {
                            ShellBottomTab.HOME -> DashboardScreen(
                                stateHolder = dashboardState,
                                onOpenMedia = shellState::openMedia,
                                onEditMedia = shellState::openEditor,
                                onOpenExplore = shellState::openExplore,
                                onOpenCalendar = shellState::openCalendar,
                            )
                            ShellBottomTab.ANIMATION -> AnimationScreen(
                                stateHolder = animationState,
                                selectedSection = shellState.selectedAnimationSection,
                                searchQuery = if (shellState.isSearchActive) shellState.searchQuery else "",
                                scrollBehavior = scrollBehavior,
                                onOpenMedia = shellState::openMedia,
                                onEditMedia = shellState::openEditor,
                                onCompletionReached = { id, progress ->
                                    shellState.openEditorForCompletion(id, progress)
                                },
                                onSearchGlobally = shellState::searchGlobally,
                                onRefresh = { animationState.load(viewer.id, forceRefresh = true) },
                            )
                            ShellBottomTab.READING -> ReadingScreen(
                                stateHolder = readingState,
                                selectedSection = shellState.selectedReadingSection,
                                searchQuery = if (shellState.isSearchActive) shellState.searchQuery else "",
                                scrollBehavior = scrollBehavior,
                                onOpenMedia = shellState::openMedia,
                                onEditMedia = shellState::openEditor,
                                onCompletionReached = { id, progress ->
                                    shellState.openEditorForCompletion(id, progress)
                                },
                                onSearchGlobally = shellState::searchGlobally,
                                onRefresh = { readingState.load(viewer.id, forceRefresh = true) },
                            )
                            ShellBottomTab.FEED -> FeedScreen(
                                stateHolder = feedState,
                                onMediaClick = { mediaId, _ -> shellState.openMedia(mediaId) },
                            )
                            ShellBottomTab.ACCOUNT -> ShellAccountRouter(
                                route = shellState.accountRoute,
                                profile = shellState.currentProfile,
                                settingsState = settingsState,
                                accountStats = accountState.stats,
                                accountFavorites = accountState.favorites,
                                accountIsRefreshing = accountState.isRefreshing,
                                onAccountRefresh = { accountState.load(viewer.id, forceRefresh = true) },
                                language = language,
                                isDarkMode = isDarkMode,
                                onLanguageChange = onLanguageChange,
                                onToggleTheme = onToggleTheme,
                                onProfileChange = { shellState.currentProfile = it },
                                onNavigate = { shellState.accountRoute = it },
                                onOpenAnimationList = { shellState.selectTab(ShellBottomTab.ANIMATION) },
                                onOpenReadingList = { shellState.selectTab(ShellBottomTab.READING) },
                                onOpenMedia = shellState::openMedia,
                                onEditMedia = shellState::openEditor,
                                onLogout = onLogout,
                                onBackHandlerChange = { shellState.topBarBackHandler = it },
                            )
                        }
                    }
                }
            }

            if (shellState.isExploreOpen) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    ExploreScreen(
                        stateHolder = exploreState,
                        showFilters = showExploreFilters,
                        onShowFiltersChange = { showExploreFilters = it },
                        onOpenMedia = shellState::openMedia,
                        onEditMedia = shellState::openEditor,
                    )
                }
            }

            if (shellState.isCalendarOpen) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    CalendarScreen(
                        stateHolder = calendarState,
                        onOpenMedia = shellState::openMedia,
                        onEditMedia = shellState::openEditor,
                    )
                }
            }

            shellState.selectedMediaId?.let { mediaId ->
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    MediaDetailScreen(
                        mediaId = mediaId,
                        stateHolder = mediaDetailState,
                        onRequestEdit = shellState::openEditor,
                        onOpenRelation = shellState::openMedia,
                    )
                }
            }

            if (!shellState.isAccountDetail && !shellState.isMediaDetailOpen && !shellState.isExploreOpen && !shellState.isCalendarOpen) {
                ShellBottomNavBar(
                    selected = shellState.selectedTab,
                    onSelect = shellState::selectTab,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                )
            }
        }

        shellState.editorMediaId?.let { editorMediaId ->
            val editorState = rememberMediaEntryEditorState(
                mediaId = editorMediaId,
                entryRepository = mediaListEntryRepository,
                detailRepository = mediaDetailRepository,
                initialStatusOverride = shellState.editorInitialStatus,
                initialProgressOverride = shellState.editorInitialProgress,
            )
            MediaDetailEditorSheet(
                state = editorState,
                onDismiss = shellState::closeEditor,
                onSaved = {
                    shellState.closeEditor()
                    // Only reload the list type that was actually edited to avoid
                    // firing 2 heavy queries when only 1 was needed.
                    // forceRefresh = true bypasses the TTL cache to reflect the edit.
                    if (editorState.isManga) {
                        readingState.load(viewer.id, forceRefresh = true)
                    } else {
                        animationState.load(viewer.id, forceRefresh = true)
                    }
                    if (shellState.selectedMediaId == editorMediaId) {
                        mediaDetailState.refresh()
                    }
                },
            )
        }

        if (showListFilterSheet) {
            val tab = shellState.selectedTab
            val sortOption = if (tab == ShellBottomTab.ANIMATION) animationState.currentSortOption else readingState.currentSortOption
            val sortOrder = if (tab == ShellBottomTab.ANIMATION) animationState.currentSortOrder else readingState.currentSortOrder
            val isPersisted = if (tab == ShellBottomTab.ANIMATION) animationState.isFilterPersisted else readingState.isFilterPersisted

            PlatformListFilterSheet(
                currentSort = sortOption,
                currentOrder = sortOrder,
                persist = isPersisted,
                isManga = tab == ShellBottomTab.READING,
                onDismiss = { showListFilterSheet = false },
                onApply = { newSort, newOrder, newPersist ->
                    if (tab == ShellBottomTab.ANIMATION) {
                        animationState.updateSort(newSort, newOrder, newPersist)
                    } else {
                        readingState.updateSort(newSort, newOrder, newPersist)
                    }
                    filterSettings.saveFilter(newSort.name, newOrder.name, newPersist)
                    showListFilterSheet = false
                }
            )
        }

        if (showFeedFilterSheet) {
            FeedFilterSheet(
                current = feedState.activityFilter,
                onDismiss = { showFeedFilterSheet = false },
                onApply = { newFilter ->
                    feedState.applyActivityFilter(newFilter)
                    showFeedFilterSheet = false
                },
            )
        }
    }
}
