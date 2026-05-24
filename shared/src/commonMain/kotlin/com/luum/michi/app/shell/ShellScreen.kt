package com.luum.michi.app.shell

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import com.luum.michi.app.account.data.AccountFavoritesRepository
import com.luum.michi.app.account.data.AccountStatsRepository
import com.luum.michi.app.account.presentation.state.rememberAccountStateHolder
import com.luum.michi.app.animation.data.AnimationListRepository
import com.luum.michi.app.animation.presentation.AnimationScreen
import com.luum.michi.app.animation.presentation.components.AnimationSectionChips
import com.luum.michi.app.animation.presentation.state.rememberAnimationListStateHolder
import com.luum.michi.app.browse.data.BrowseRepository
import com.luum.michi.app.browse.presentation.BrowseScreen
import com.luum.michi.app.browse.presentation.state.rememberBrowseStateHolder
import com.luum.michi.app.calendar.data.CalendarRepository
import com.luum.michi.app.calendar.presentation.CalendarScreen
import com.luum.michi.app.calendar.presentation.state.rememberCalendarStateHolder
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformSystemBackHandler
import com.luum.michi.app.core.session.Viewer
import com.luum.michi.app.discovery.data.DiscoveryRepository
import com.luum.michi.app.discovery.presentation.DiscoveryScreen
import com.luum.michi.app.discovery.presentation.state.rememberDiscoveryStateHolder
import com.luum.michi.app.mediaDetail.data.MediaDetailRepository
import com.luum.michi.app.mediaDetail.data.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.presentation.MediaDetailScreen
import com.luum.michi.app.mediaDetail.presentation.components.MediaDetailEditorSheet
import com.luum.michi.app.mediaDetail.presentation.state.rememberMediaDetailStateHolder
import com.luum.michi.app.mediaDetail.presentation.state.rememberMediaEntryEditorState
import com.luum.michi.app.reading.data.ReadingListRepository
import com.luum.michi.app.reading.presentation.ReadingScreen
import com.luum.michi.app.reading.presentation.components.ReadingSectionChips
import com.luum.michi.app.reading.presentation.state.rememberReadingListStateHolder
import com.luum.michi.app.search.data.SearchRepository
import com.luum.michi.app.search.presentation.SearchScreen
import com.luum.michi.app.search.presentation.state.rememberSearchStateHolder
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
internal fun ShellScreen(
    viewer: Viewer,
    animationListRepository: AnimationListRepository,
    readingListRepository: ReadingListRepository,
    accountStatsRepository: AccountStatsRepository,
    accountFavoritesRepository: AccountFavoritesRepository,
    discoveryRepository: DiscoveryRepository,
    browseRepository: BrowseRepository,
    calendarRepository: CalendarRepository,
    mediaDetailRepository: MediaDetailRepository,
    mediaListEntryRepository: MediaListEntryRepository,
    searchRepository: SearchRepository,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
) {
    val strings = LanguageProvider.strings
    val shellState = rememberShellState(viewer)
    val animationState = rememberAnimationListStateHolder(animationListRepository, viewer.id)
    val readingState = rememberReadingListStateHolder(readingListRepository, viewer.id)
    val accountState = rememberAccountStateHolder(
        statsRepository = accountStatsRepository,
        favoritesRepository = accountFavoritesRepository,
        viewerId = viewer.id,
    )
    val discoveryState = rememberDiscoveryStateHolder(discoveryRepository)
    val browseState = rememberBrowseStateHolder(browseRepository)
    val calendarState = rememberCalendarStateHolder(calendarRepository)
    val mediaDetailState = rememberMediaDetailStateHolder(mediaDetailRepository)
    val searchState = rememberSearchStateHolder(searchRepository)
    val settingsState = rememberSettingsState()

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
        enabled = !shellState.isEditorOpen && !shellState.isMediaDetailOpen && shellState.isBrowseOpen,
        onBack = shellState::closeBrowse,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isMediaDetailOpen && !shellState.isBrowseOpen &&
            shellState.isCalendarOpen,
        onBack = shellState::closeCalendar,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isMediaDetailOpen && !shellState.isBrowseOpen &&
            !shellState.isCalendarOpen && shellState.isAccountDetail,
        onBack = shellState::handleAccountBack,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isMediaDetailOpen && !shellState.isBrowseOpen &&
            !shellState.isCalendarOpen && shellState.isSearchTab && shellState.isSearchActive,
        onBack = shellState::closeSearch,
    )

    val titleText = when {
        shellState.isMediaDetailOpen -> strings.mediaDetailTitle
        shellState.isBrowseOpen -> strings.browseTitle
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
                isBrowseOpen = shellState.isBrowseOpen,
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
                onBrowseBack = shellState::closeBrowse,
                onCalendarBack = shellState::closeCalendar,
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
            if (shellState.isSearchActive && shellState.isSearchTab) {
                SearchScreen(
                    query = shellState.searchQuery,
                    stateHolder = searchState,
                    onOpenMedia = shellState::openMedia,
                    onEditMedia = shellState::openEditor,
                )
            } else when (shellState.selectedTab) {
                ShellBottomTab.HOME -> DiscoveryScreen(
                    stateHolder = discoveryState,
                    onOpenMedia = shellState::openMedia,
                    onEditMedia = shellState::openEditor,
                    onOpenBrowse = shellState::openBrowse,
                    onOpenCalendar = shellState::openCalendar,
                )
                ShellBottomTab.ANIMATION -> AnimationScreen(
                    stateHolder = animationState,
                    selectedSection = shellState.selectedAnimationSection,
                    scrollBehavior = scrollBehavior,
                    onOpenMedia = shellState::openMedia,
                    onEditMedia = shellState::openEditor,
                )
                ShellBottomTab.READING -> ReadingScreen(
                    stateHolder = readingState,
                    selectedSection = shellState.selectedReadingSection,
                    scrollBehavior = scrollBehavior,
                    onOpenMedia = shellState::openMedia,
                    onEditMedia = shellState::openEditor,
                )
                ShellBottomTab.ACCOUNT -> ShellAccountRouter(
                    route = shellState.accountRoute,
                    profile = shellState.currentProfile,
                    settingsState = settingsState,
                    accountStats = accountState.stats,
                    accountFavorites = accountState.favorites,
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

            if (shellState.isBrowseOpen) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    BrowseScreen(
                        stateHolder = browseState,
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
                    )
                }
            }
        }

        shellState.editorMediaId?.let { editorMediaId ->
            val editorState = rememberMediaEntryEditorState(
                mediaId = editorMediaId,
                entryRepository = mediaListEntryRepository,
                detailRepository = mediaDetailRepository,
            )
            MediaDetailEditorSheet(
                state = editorState,
                onDismiss = shellState::closeEditor,
                onSaved = {
                    shellState.closeEditor()
                    animationState.load(viewer.id)
                    readingState.load(viewer.id)
                    if (shellState.selectedMediaId == editorMediaId) {
                        mediaDetailState.refresh()
                    }
                },
            )
        }
    }
}
