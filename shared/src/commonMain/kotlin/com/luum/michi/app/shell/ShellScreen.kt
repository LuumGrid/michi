package com.luum.michi.app.shell

import androidx.compose.foundation.background
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.luum.michi.app.account.domain.AccountRepository
import com.luum.michi.app.account.domain.model.AccountFavoritesCategory
import com.luum.michi.app.account.presentation.AccountSettingsSheet
import com.luum.michi.app.account.presentation.AccountShareProfileSheet
import com.luum.michi.app.account.presentation.state.rememberAccountFavoritesGridStateHolder
import com.luum.michi.app.account.presentation.state.rememberAccountStateHolder
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.presentation.anime.AnimeScreen
import com.luum.michi.app.mediaList.domain.anime.model.AnimeListSection
import com.luum.michi.app.mediaList.domain.anime.model.label
import com.luum.michi.app.mediaList.domain.manga.model.MangaListSection
import com.luum.michi.app.mediaList.domain.manga.model.label
import com.luum.michi.app.mediaList.presentation.anime.state.rememberAnimeListStateHolder
import com.luum.michi.app.discover.domain.DashboardRepository
import com.luum.michi.app.discover.presentation.dashboard.DashboardRail
import com.luum.michi.app.discover.presentation.dashboard.DashboardScreen
import com.luum.michi.app.discover.presentation.dashboard.state.rememberDashboardStateHolder
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.presentation.explore.ExploreScreen
import com.luum.michi.app.discover.presentation.explore.ExploreFilterSheet
import com.luum.michi.app.discover.presentation.explore.state.ExploreCategory
import com.luum.michi.app.discover.presentation.explore.state.rememberExploreStateHolder
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.presentation.CalendarScreen
import com.luum.michi.app.calendar.presentation.state.rememberCalendarStateHolder
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.media.currentSeasonAndYear
import com.luum.michi.app.core.media.next
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformSystemBackHandler
import com.luum.michi.app.core.session.Viewer
import com.luum.michi.app.mediaDetail.domain.MediaDetailRepository
import com.luum.michi.app.core.medialist.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.presentation.MediaDetailScreen
import com.luum.michi.app.mediaDetail.presentation.components.MediaDetailEditorSheet
import com.luum.michi.app.mediaDetail.presentation.state.rememberMediaDetailStateHolder
import com.luum.michi.app.mediaDetail.presentation.state.rememberMediaEntryEditorState
import com.luum.michi.app.characterDetail.domain.CharacterDetailRepository
import com.luum.michi.app.characterDetail.presentation.CharacterDetailScreen
import com.luum.michi.app.characterDetail.presentation.state.rememberCharacterDetailStateHolder
import com.luum.michi.app.staffDetail.domain.StaffDetailRepository
import com.luum.michi.app.staffDetail.presentation.StaffDetailScreen
import com.luum.michi.app.staffDetail.presentation.state.rememberStaffDetailStateHolder
import com.luum.michi.app.studioDetail.domain.StudioDetailRepository
import com.luum.michi.app.studioDetail.presentation.StudioDetailScreen
import com.luum.michi.app.studioDetail.presentation.state.rememberStudioDetailStateHolder
import com.luum.michi.app.notifications.domain.NotificationsRepository
import com.luum.michi.app.notifications.presentation.NotificationsScreen
import com.luum.michi.app.notifications.presentation.model.NotificationTarget
import com.luum.michi.app.notifications.presentation.state.rememberNotificationsStateHolder
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.mediaList.presentation.manga.MangaScreen
import com.luum.michi.app.mediaList.presentation.manga.state.rememberMangaListStateHolder
import com.luum.michi.app.settings.domain.SettingsRepository
import com.luum.michi.app.settings.presentation.model.DiscoverTabOption
import com.luum.michi.app.settings.presentation.state.rememberSettingsState
import com.luum.michi.app.shell.components.ShellAccountRouter
import com.luum.michi.app.core.platform.SettingsStoreKeys
import com.luum.michi.app.core.platform.components.PlatformFilterSheet
import com.luum.michi.app.core.platform.components.PlatformFilterGroup
import com.luum.michi.app.core.platform.components.PlatformFilterOption
import com.luum.michi.app.core.platform.rememberPlatformFilterSettings
import com.luum.michi.app.core.platform.rememberPlatformSettingsStore
import com.luum.michi.app.core.platform.components.PlatformListFilterSheet
import com.luum.michi.app.core.platform.model.UserListSort
import com.luum.michi.app.core.platform.model.UserListOrder
import com.luum.michi.app.shell.components.ShellBottomCluster
import com.luum.michi.app.shell.components.ShellTabSection
import com.luum.michi.app.shell.components.label
import com.luum.michi.app.shell.components.ShellToolBar
import com.luum.michi.app.shell.state.DetailDestination
import com.luum.michi.app.shell.state.ShellAccountRoute
import com.luum.michi.app.shell.state.rememberShellState

@Composable
internal fun ShellScreen(
    viewer: Viewer,
    animeListRepository: AnimeListRepository,
    mangaListRepository: MangaListRepository,
    accountRepository: AccountRepository,
    dashboardRepository: DashboardRepository,
    exploreRepository: ExploreRepository,
    calendarRepository: CalendarRepository,
    mediaDetailRepository: MediaDetailRepository,
    mediaListEntryRepository: MediaListEntryRepository,
    notificationsRepository: NotificationsRepository,
    studioDetailRepository: StudioDetailRepository,
    characterDetailRepository: CharacterDetailRepository,
    staffDetailRepository: StaffDetailRepository,
    settingsRepository: SettingsRepository,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
) {
    val strings = LanguageProvider.strings
    val settingsStore = rememberPlatformSettingsStore()
    val initialTab = remember(settingsStore) {
        settingsStore.getString(SettingsStoreKeys.DefaultDiscoverTab)
            ?.let { name ->
                DiscoverTabOption.entries.firstOrNull { it.name == name }
                    ?: when (name) {
                        "HOME" -> DiscoverTabOption.DISCOVER
                        "ANIMATION" -> DiscoverTabOption.ANIME
                        "READING" -> DiscoverTabOption.MANGA
                        else -> null
                    }
            }
            ?.toShellTab() ?: ShellTabSection.DISCOVER
    }
    val shellState = rememberShellState(viewer, initialTab)
    val animeState = rememberAnimeListStateHolder(animeListRepository, mediaListEntryRepository, viewer.id)
    val mangaState = rememberMangaListStateHolder(mangaListRepository, mediaListEntryRepository, viewer.id)
    val accountState = rememberAccountStateHolder(
        repository = accountRepository,
        viewerId = viewer.id,
    )
    val favoritesGridState = rememberAccountFavoritesGridStateHolder(accountRepository)
    val dashboardState = rememberDashboardStateHolder(dashboardRepository)
    val exploreState = rememberExploreStateHolder(exploreRepository)
    val calendarState = rememberCalendarStateHolder(calendarRepository)
    val mediaDetailState = rememberMediaDetailStateHolder(mediaDetailRepository)
    val studioDetailState = rememberStudioDetailStateHolder(studioDetailRepository, viewerId = viewer.id)
    val characterDetailState = rememberCharacterDetailStateHolder(characterDetailRepository, viewerId = viewer.id)
    val staffDetailState = rememberStaffDetailStateHolder(staffDetailRepository, viewerId = viewer.id)
    val notificationsState = rememberNotificationsStateHolder(notificationsRepository)
    val settingsState = rememberSettingsState(settingsRepository, settingsStore)
    val uriHandler = LocalUriHandler.current
    var showListFilterSheet by remember { mutableStateOf(false) }

    // Discover es tab: los rails "Ver todo" preseleccionan categoría + orden
    // en la misma view, limpiando los demás filtros.
    fun openExploreWith(
        category: ExploreCategory,
        sortOption: UserListSort,
        season: String? = null,
        year: Int? = null,
    ) {
        exploreState.applyPreset(category, sortOption, season = season, year = year)
        shellState.openExplore()
    }

    val filterSettings = rememberPlatformFilterSettings()
    LaunchedEffect(Unit) {
        filterSettings.loadFilter()?.let { (sortName, orderName, persist) ->
            val sort = UserListSort.entries.firstOrNull { it.name == sortName } ?: UserListSort.FOLLOW_LIST
            val order = UserListOrder.entries.firstOrNull { it.name == orderName } ?: UserListOrder.DESCENDING
            animeState.updateSort(sort, order, persist)
            mangaState.updateSort(sort, order, persist)
        }
    }

    val tabStateHolder = rememberSaveableStateHolder()
    val detailStateHolder = rememberSaveableStateHolder()

    LaunchedEffect(shellState.selectedTab) {
        when (shellState.selectedTab) {
            ShellTabSection.ANIME -> {
                if (animeState.entries.isEmpty() && !animeState.isLoading) {
                    animeState.load(viewer.id)
                }
            }
            ShellTabSection.MANGA -> {
                if (mangaState.entries.isEmpty() && !mangaState.isLoading) {
                    mangaState.load(viewer.id)
                }
            }
            ShellTabSection.ACCOUNT -> {
                if (accountState.stats.animeCount == 0 && !accountState.isLoading) {
                    accountState.load(viewer.id)
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(shellState.accountRoute, shellState.favoritesCategory) {
        if (shellState.accountRoute == ShellAccountRoute.FAVORITES) {
            favoritesGridState.load(viewer.id, shellState.favoritesCategory)
        }
    }

    PlatformSystemBackHandler(
        enabled = shellState.isEditorOpen,
        onBack = shellState::closeEditor,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && shellState.isDetailOpen,
        onBack = shellState::closeDetail,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isDetailOpen && shellState.isExploreOpen,
        onBack = {
            if (shellState.isExploreFilterOpen) {
                shellState.closeExploreFilter()
            } else {
                shellState.closeExplore()
            }
        },
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isDetailOpen && !shellState.isExploreOpen &&
                shellState.isCalendarOpen,
        onBack = shellState::closeCalendar,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isDetailOpen && !shellState.isExploreOpen &&
                !shellState.isCalendarOpen && shellState.isNotificationsOpen,
        onBack = shellState::closeNotifications,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isDetailOpen && !shellState.isExploreOpen &&
                !shellState.isCalendarOpen && !shellState.isNotificationsOpen && shellState.isAccountDetail,
        onBack = shellState::handleAccountBack,
    )
    val titleText = when {
        shellState.currentDetail is DetailDestination.Character -> strings.characterDetailTitle
        shellState.currentDetail is DetailDestination.Studio -> strings.studioDetailTitle
        shellState.currentDetail is DetailDestination.Staff -> strings.staffDetailTitle
        shellState.isDetailOpen -> strings.mediaDetailTitle
        shellState.isCalendarOpen -> strings.calendarTitle
        shellState.isNotificationsOpen -> strings.notificationsAction
        shellState.selectedTab == ShellTabSection.ACCOUNT &&
            shellState.accountRoute == ShellAccountRoute.SETTINGS -> strings.settingsAction
        shellState.selectedTab == ShellTabSection.ACCOUNT &&
            shellState.accountRoute == ShellAccountRoute.STATS -> strings.accountStatsTitle
        shellState.selectedTab == ShellTabSection.ACCOUNT &&
                shellState.accountRoute == ShellAccountRoute.FAVORITES -> when (shellState.favoritesCategory) {
            AccountFavoritesCategory.ANIME -> strings.accountFavoriteAnimeTitle
            AccountFavoritesCategory.MANGA -> strings.accountFavoriteMangaTitle
            AccountFavoritesCategory.CHARACTERS -> strings.accountFavoriteCharactersTitle
            AccountFavoritesCategory.STAFF -> strings.accountFavoriteStaffTitle
            AccountFavoritesCategory.STUDIOS -> strings.accountFavoriteStudiosTitle
        }
        shellState.selectedTab == ShellTabSection.ACCOUNT ->
            shellState.currentProfile.username
        else -> shellState.selectedTab.label(strings)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
    ) {
        // Contenido full-bleed: la toolbar y el bottom cluster flotan por encima.
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = shellState.selectedTab,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                modifier = Modifier.fillMaxSize(),
                label = "tabCrossfade",
            ) { tab ->
                tabStateHolder.SaveableStateProvider(tab) {
                    when (tab) {
                        ShellTabSection.DISCOVER -> Crossfade(
                            targetState = shellState.isExploreOpen,
                            label = "discoverExploreCrossfade",
                        ) { showingExplore ->
                            if (showingExplore) {
                                ExploreScreen(
                                    stateHolder = exploreState,
                                    onOpenMedia = shellState::openMedia,
                                    onEditMedia = shellState::openEditor,
                                )
                            } else {
                                DashboardScreen(
                                    stateHolder = dashboardState,
                                    onOpenMedia = shellState::openMedia,
                                    onEditMedia = shellState::openEditor,
                                    onSeeAll = { rail ->
                                        when (rail) {
                                            DashboardRail.THIS_SEASON -> {
                                                val current = currentSeasonAndYear()
                                                openExploreWith(
                                                    ExploreCategory.ANIME,
                                                    UserListSort.POPULARITY,
                                                    season = current.season.name,
                                                    year = current.year,
                                                )
                                            }
                                            DashboardRail.UPCOMING_NEXT_SEASON -> {
                                                val upcoming = currentSeasonAndYear().next()
                                                openExploreWith(
                                                    ExploreCategory.ANIME,
                                                    UserListSort.POPULARITY,
                                                    season = upcoming.season.name,
                                                    year = upcoming.year,
                                                )
                                            }
                                            DashboardRail.TRENDING_ANIME ->
                                                openExploreWith(ExploreCategory.ANIME, UserListSort.TRENDING)
                                            DashboardRail.TRENDING_MANGA ->
                                                openExploreWith(ExploreCategory.MANGA, UserListSort.TRENDING)
                                            DashboardRail.ALL_TIME_POPULAR_ANIME ->
                                                openExploreWith(ExploreCategory.ANIME, UserListSort.POPULARITY)
                                            DashboardRail.ALL_TIME_POPULAR_MANGA ->
                                                openExploreWith(ExploreCategory.MANGA, UserListSort.POPULARITY)
                                            DashboardRail.TOP_ANIME ->
                                                openExploreWith(ExploreCategory.ANIME, UserListSort.AVERAGE_SCORE)
                                            DashboardRail.TOP_MANGA ->
                                                openExploreWith(ExploreCategory.MANGA, UserListSort.AVERAGE_SCORE)
                                        }
                                    },
                                )
                            }
                        }
                        ShellTabSection.ANIME -> AnimeScreen(
                            stateHolder = animeState,
                            selectedSection = shellState.selectedAnimeSection,
                            onOpenMedia = shellState::openMedia,
                            onEditMedia = shellState::openEditor,
                            onCompletionReached = { id, progress ->
                                shellState.openEditorForCompletion(id, progress)
                            },
                            onRefresh = { animeState.load(viewer.id, forceRefresh = true) },
                        )
                        ShellTabSection.MANGA -> MangaScreen(
                            stateHolder = mangaState,
                            selectedSection = shellState.selectedMangaSection,
                            onOpenMedia = shellState::openMedia,
                            onEditMedia = shellState::openEditor,
                            onCompletionReached = { id, progress ->
                                shellState.openEditorForCompletion(id, progress)
                            },
                            onRefresh = { mangaState.load(viewer.id, forceRefresh = true) },
                        )
                        ShellTabSection.ACCOUNT -> ShellAccountRouter(
                            route = shellState.accountRoute,
                            profile = shellState.currentProfile,
                            settingsState = settingsState,
                            accountState = accountState,
                            shellState = shellState,
                            favoritesCategory = shellState.favoritesCategory,
                            favoritesGridStateHolder = favoritesGridState,
                            language = language,
                            isDarkMode = isDarkMode,
                            onLanguageChange = onLanguageChange,
                            onToggleTheme = onToggleTheme,
                            onNavigate = { shellState.accountRoute = it },
                            onOpenAnimeList = { shellState.selectTab(ShellTabSection.ANIME) },
                            onOpenMangaList = { shellState.selectTab(ShellTabSection.MANGA) },
                            onOpenMedia = shellState::openMedia,
                            onEditMedia = shellState::openEditor,
                            onOpenCharacter = shellState::openCharacter,
                            onOpenStaff = shellState::openStaff,
                            onOpenStudio = shellState::openStudio,
                            onOpenFavoritesGrid = shellState::openFavoritesGrid,
                            onLogout = onLogout,
                            onBackHandlerChange = { shellState.toolBarBackHandler = it },
                        )
                    }
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

            if (shellState.isNotificationsOpen) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    NotificationsScreen(
                        stateHolder = notificationsState,
                        onOpen = { target ->
                            when (target) {
                                is NotificationTarget.Media -> {
                                    shellState.closeNotifications()
                                    shellState.openMedia(target.id)
                                }
                                is NotificationTarget.Web -> uriHandler.openUri(target.url)
                                NotificationTarget.None -> {}
                            }
                        },
                    )
                }
            }

            when (val dest = shellState.currentDetail) {
                is DetailDestination.Media -> detailStateHolder.SaveableStateProvider("media-${dest.id}") {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        MediaDetailScreen(
                            mediaId = dest.id,
                            stateHolder = mediaDetailState,
                            onRequestEdit = shellState::openEditor,
                            onOpenRelation = shellState::openMedia,
                            onOpenStudio = shellState::openStudio,
                            onOpenCharacter = shellState::openCharacter,
                            onOpenStaff = shellState::openStaff,
                        )
                    }
                }
                is DetailDestination.Studio -> detailStateHolder.SaveableStateProvider("studio-${dest.id}") {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        StudioDetailScreen(
                            id = dest.id,
                            stateHolder = studioDetailState,
                            onOpenMedia = shellState::openMedia,
                        )
                    }
                }
                is DetailDestination.Character -> detailStateHolder.SaveableStateProvider("character-${dest.id}") {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        CharacterDetailScreen(
                            id = dest.id,
                            stateHolder = characterDetailState,
                            onOpenMedia = shellState::openMedia,
                            onOpenStaff = shellState::openStaff,
                        )
                    }
                }
                is DetailDestination.Staff -> detailStateHolder.SaveableStateProvider("staff-${dest.id}") {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        StaffDetailScreen(
                            id = dest.id,
                            stateHolder = staffDetailState,
                            onOpenMedia = shellState::openMedia,
                            onOpenCharacter = shellState::openCharacter,
                        )
                    }
                }
                else -> {}
            }

            if (!shellState.isAccountDetail && !shellState.isDetailOpen && !shellState.isCalendarOpen && !shellState.isNotificationsOpen) {
                ShellBottomCluster(
                    selectedTab = shellState.selectedTab,
                    onSearchClick = {
                        exploreState.enterBlankSearch()
                        shellState.openExplore()
                    },
                    onSelectTab = shellState::selectTab,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // Toolbar global flotante: overlay dibujado al final para quedar sobre el contenido.
            ShellToolBar(
                selectedTab = shellState.selectedTab,
                isAccountDetail = shellState.isAccountDetail,
                isDetailOpen = shellState.isDetailOpen,
                isExploreOpen = shellState.isExploreOpen,
                isCalendarOpen = shellState.isCalendarOpen,
                isNotificationsOpen = shellState.isNotificationsOpen,
                titleText = titleText,
                onAccountBack = shellState::handleAccountBack,
                onMediaBack = shellState::closeDetail,
                onExploreBack = {
                    if (shellState.isExploreFilterOpen) {
                        shellState.closeExploreFilter()
                    } else {
                        shellState.closeExplore()
                    }
                },
                onCalendarBack = shellState::closeCalendar,
                onNotificationsBack = shellState::closeNotifications,
                onOpenSettings = { shellState.accountRoute = ShellAccountRoute.SETTINGS },
                onNotificationsClick = {
                    shellState.openNotifications()
                    notificationsState.load(resetCount = true)
                },
                onSortClick = { showListFilterSheet = true },
                onSectionFilterClick = {
                    if (shellState.selectedTab == ShellTabSection.DISCOVER) {
                        shellState.openExploreFilter()
                    } else {
                        shellState.openSectionFilter()
                    }
                },
                onOpenCalendar = shellState::openCalendar,
                onOpenExplore = { openExploreWith(ExploreCategory.ANIME, UserListSort.TRENDING) },
                exploreQuery = exploreState.query,
                onExploreQueryChange = { exploreState.updateFilters(newQuery = it) },
                exploreFocusRequested = exploreState.focusSearchRequested,
                onExploreFocusConsumed = exploreState::consumeFocusRequest,
                onExploreFilterClick = shellState::openExploreFilter,
                unreadCount = notificationsState.unreadCount,
                modifier = Modifier.align(Alignment.TopCenter),
            )
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
                        mangaState.load(viewer.id, forceRefresh = true)
                    } else {
                        animeState.load(viewer.id, forceRefresh = true)
                    }
                    if (shellState.selectedMediaId == editorMediaId) {
                        mediaDetailState.refresh()
                    }
                },
                onDeleted = {
                    shellState.closeEditor()
                    if (editorState.isManga) {
                        mangaState.load(viewer.id, forceRefresh = true)
                    } else {
                        animeState.load(viewer.id, forceRefresh = true)
                    }
                    if (shellState.selectedMediaId == editorMediaId) {
                        mediaDetailState.refresh()
                    }
                },
            )
        }

        if (showListFilterSheet) {
            val tab = shellState.selectedTab
            val sortOption = when (tab) {
                ShellTabSection.DISCOVER -> exploreState.currentSortOption
                ShellTabSection.ANIME -> animeState.currentSortOption
                ShellTabSection.MANGA -> mangaState.currentSortOption
                else -> animeState.currentSortOption
            }
            val sortOrder = when (tab) {
                ShellTabSection.DISCOVER -> exploreState.currentSortOrder
                ShellTabSection.ANIME -> animeState.currentSortOrder
                ShellTabSection.MANGA -> mangaState.currentSortOrder
                else -> animeState.currentSortOrder
            }
            val isPersisted = when (tab) {
                ShellTabSection.DISCOVER -> exploreState.isFilterPersisted
                ShellTabSection.ANIME -> animeState.isFilterPersisted
                ShellTabSection.MANGA -> mangaState.isFilterPersisted
                else -> false
            }

            PlatformListFilterSheet(
                currentSort = sortOption,
                currentOrder = sortOrder,
                persist = isPersisted,
                isManga = tab == ShellTabSection.MANGA ||
                        (tab == ShellTabSection.DISCOVER && exploreState.category == ExploreCategory.MANGA),
                onDismiss = { showListFilterSheet = false },
                onApply = { newSort, newOrder, newPersist ->
                    when (tab) {
                        ShellTabSection.DISCOVER -> exploreState.updateSort(newSort, newOrder, newPersist)
                        ShellTabSection.ANIME -> {
                            animeState.updateSort(newSort, newOrder, newPersist)
                            filterSettings.saveFilter(newSort.name, newOrder.name, newPersist)
                        }
                        ShellTabSection.MANGA -> {
                            mangaState.updateSort(newSort, newOrder, newPersist)
                            filterSettings.saveFilter(newSort.name, newOrder.name, newPersist)
                        }
                        else -> {}
                    }
                    showListFilterSheet = false
                }
            )
        }

        if (shellState.isExploreFilterOpen) {
            ExploreFilterSheet(
                stateHolder = exploreState,
                onDismiss = shellState::closeExploreFilter,
            )
        }

        if (shellState.isAccountSettingsOpen) {
            AccountSettingsSheet(onDismiss = shellState::closeAccountSettings)
        }

        if (shellState.isShareProfileOpen) {
            AccountShareProfileSheet(
                username = shellState.currentProfile.username,
                displayName = shellState.currentProfile.displayName,
                avatarUrl = shellState.currentProfile.avatarUrl,
                onDismiss = shellState::closeShareProfile,
            )
        }

        if (shellState.isSectionFilterOpen) {
            val sectionOptions: List<PlatformFilterOption>?
            val selectedSectionId: String?
            when (shellState.selectedTab) {
                ShellTabSection.ANIME -> {
                    sectionOptions = AnimeListSection.entries.map { section ->
                        PlatformFilterOption(
                            id = section.name,
                            label = section.label(strings),
                            count = animeState.countInSection(section),
                        )
                    }
                    selectedSectionId = shellState.selectedAnimeSection.name
                }
                ShellTabSection.MANGA -> {
                    sectionOptions = MangaListSection.entries.map { section ->
                        PlatformFilterOption(
                            id = section.name,
                            label = section.label(strings),
                            count = mangaState.countInSection(section),
                        )
                    }
                    selectedSectionId = shellState.selectedMangaSection.name
                }
                else -> {
                    sectionOptions = null
                    selectedSectionId = null
                }
            }
            if (sectionOptions != null && selectedSectionId != null) {
                PlatformFilterSheet(
                    groups = listOf(
                        PlatformFilterGroup(
                            id = "section",
                            title = null,
                            options = sectionOptions,
                            selectedId = selectedSectionId,
                        ),
                    ),
                    onSelect = { _, id ->
                        when (shellState.selectedTab) {
                            ShellTabSection.ANIME -> AnimeListSection.entries
                                .firstOrNull { it.name == id }
                                ?.let { shellState.selectedAnimeSection = it }
                            ShellTabSection.MANGA -> MangaListSection.entries
                                .firstOrNull { it.name == id }
                                ?.let { shellState.selectedMangaSection = it }
                            else -> {}
                        }
                        shellState.closeSectionFilter()
                    },
                    onDismiss = shellState::closeSectionFilter,
                )
            }
        }
    }
}

/** Maps the persisted "default discover tab" preference onto the shell's tab-bar sections. */
private fun DiscoverTabOption.toShellTab(): ShellTabSection = when (this) {
    DiscoverTabOption.DISCOVER -> ShellTabSection.DISCOVER
    DiscoverTabOption.ANIME -> ShellTabSection.ANIME
    DiscoverTabOption.MANGA -> ShellTabSection.MANGA
    DiscoverTabOption.ACCOUNT -> ShellTabSection.ACCOUNT
}