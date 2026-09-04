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
import com.luum.michi.app.account.data.AccountRepository
import com.luum.michi.app.account.presentation.model.AccountFavoritesCategory
import com.luum.michi.app.account.presentation.state.rememberAccountFavoritesGridStateHolder
import com.luum.michi.app.account.presentation.state.rememberAccountStateHolder
import com.luum.michi.app.anime.data.AnimeListRepository
import com.luum.michi.app.anime.presentation.AnimeScreen
import com.luum.michi.app.anime.presentation.model.AnimeListSection
import com.luum.michi.app.anime.presentation.model.label
import com.luum.michi.app.manga.presentation.model.MangaListSection
import com.luum.michi.app.manga.presentation.model.label
import com.luum.michi.app.anime.presentation.state.rememberAnimeListStateHolder
import com.luum.michi.app.discover.data.DiscoverRepository
import com.luum.michi.app.discover.presentation.DiscoverScreen
import com.luum.michi.app.discover.presentation.components.DiscoverFiltersSheet
import com.luum.michi.app.discover.presentation.model.applyFilterSelection
import com.luum.michi.app.discover.presentation.model.buildDiscoverFilterGroups
import com.luum.michi.app.discover.presentation.state.DiscoverCategory
import com.luum.michi.app.discover.presentation.state.rememberDiscoverStateHolder
import com.luum.michi.app.calendar.data.CalendarRepository
import com.luum.michi.app.calendar.presentation.CalendarScreen
import com.luum.michi.app.calendar.presentation.state.rememberCalendarStateHolder
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.media.currentSeasonAndYear
import com.luum.michi.app.core.media.next
import com.luum.michi.app.seasonal.presentation.SeasonalScreen
import com.luum.michi.app.seasonal.presentation.state.rememberSeasonalStateHolder
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformSystemBackHandler
import com.luum.michi.app.core.session.Viewer
import com.luum.michi.app.dashboard.data.DashboardRepository
import com.luum.michi.app.dashboard.presentation.DashboardRail
import com.luum.michi.app.dashboard.presentation.DashboardScreen
import com.luum.michi.app.dashboard.presentation.state.rememberDashboardStateHolder
import com.luum.michi.app.mediaDetail.data.MediaDetailRepository
import com.luum.michi.app.mediaDetail.data.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.presentation.MediaDetailScreen
import com.luum.michi.app.mediaDetail.presentation.components.MediaDetailEditorSheet
import com.luum.michi.app.mediaDetail.presentation.state.rememberMediaDetailStateHolder
import com.luum.michi.app.mediaDetail.presentation.state.rememberMediaEntryEditorState
import com.luum.michi.app.characterDetail.data.CharacterDetailRepository
import com.luum.michi.app.characterDetail.presentation.CharacterDetailScreen
import com.luum.michi.app.characterDetail.presentation.state.rememberCharacterDetailStateHolder
import com.luum.michi.app.staffDetail.data.StaffDetailRepository
import com.luum.michi.app.staffDetail.presentation.StaffDetailScreen
import com.luum.michi.app.staffDetail.presentation.state.rememberStaffDetailStateHolder
import com.luum.michi.app.studioDetail.data.StudioDetailRepository
import com.luum.michi.app.studioDetail.presentation.StudioDetailScreen
import com.luum.michi.app.studioDetail.presentation.state.rememberStudioDetailStateHolder
import com.luum.michi.app.notifications.data.NotificationsRepository
import com.luum.michi.app.notifications.presentation.NotificationsScreen
import com.luum.michi.app.notifications.presentation.model.NotificationTarget
import com.luum.michi.app.notifications.presentation.state.rememberNotificationsStateHolder
import com.luum.michi.app.manga.data.MangaListRepository
import com.luum.michi.app.manga.presentation.MangaScreen
import com.luum.michi.app.manga.presentation.state.rememberMangaListStateHolder
import com.luum.michi.app.settings.data.SettingsRepository
import com.luum.michi.app.settings.presentation.model.DiscoverTabOption
import com.luum.michi.app.settings.presentation.state.rememberSettingsState
import com.luum.michi.app.shell.components.ShellAccountRouter
import com.luum.michi.app.core.platform.SettingsStoreKeys
import com.luum.michi.app.core.platform.rememberPlatformFilterSettings
import com.luum.michi.app.core.platform.rememberPlatformSettingsStore
import com.luum.michi.app.core.platform.components.PlatformListFilterSheet
import com.luum.michi.app.core.platform.model.UserListSort
import com.luum.michi.app.core.platform.model.UserListOrder
import com.luum.michi.app.shell.components.ShellBottomCluster
import com.luum.michi.app.shell.components.ShellSectionFilterSheet
import com.luum.michi.app.shell.components.ShellSectionOption
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
    discoverRepository: DiscoverRepository,
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
    val discoverState = rememberDiscoverStateHolder(discoverRepository)
    val calendarState = rememberCalendarStateHolder(calendarRepository)
    val seasonalState = rememberSeasonalStateHolder(discoverRepository)
    val mediaDetailState = rememberMediaDetailStateHolder(mediaDetailRepository)
    val studioDetailState = rememberStudioDetailStateHolder(studioDetailRepository, viewerId = viewer.id)
    val characterDetailState = rememberCharacterDetailStateHolder(characterDetailRepository, viewerId = viewer.id)
    val staffDetailState = rememberStaffDetailStateHolder(staffDetailRepository, viewerId = viewer.id)
    val notificationsState = rememberNotificationsStateHolder(notificationsRepository)
    val settingsState = rememberSettingsState(settingsRepository, settingsStore)
    val uriHandler = LocalUriHandler.current
    var showSeasonalSort by remember { mutableStateOf(false) }
    var showListFilterSheet by remember { mutableStateOf(false) }
    var showDiscoverSortDropdown by remember { mutableStateOf(false) }

    // Discover es tab: los rails "Ver todo" preseleccionan categoría + orden
    // en la misma view, limpiando los demás filtros.
    val openDiscoverWith: (DiscoverCategory, String) -> Unit = { category, sort ->
        discoverState.updateFilters(
            newQuery = "",
            newCategory = category,
            newGenre = "All",
            newFormat = "All",
            newYear = null,
            newSort = sort,
        )
        shellState.openDiscover()
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
        enabled = !shellState.isEditorOpen && !shellState.isDetailOpen && shellState.isDiscoverOpen,
        onBack = {
            if (showDiscoverSortDropdown) {
                showDiscoverSortDropdown = false
            } else {
                shellState.closeDiscover()
            }
        },
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isDetailOpen && !shellState.isDiscoverOpen &&
            shellState.isCalendarOpen,
        onBack = shellState::closeCalendar,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isDetailOpen && !shellState.isDiscoverOpen &&
            !shellState.isCalendarOpen && shellState.isSeasonalOpen,
        onBack = {
            if (showSeasonalSort) {
                showSeasonalSort = false
            } else {
                shellState.closeSeasonal()
            }
        },
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isDetailOpen && !shellState.isDiscoverOpen &&
            !shellState.isCalendarOpen && !shellState.isSeasonalOpen && shellState.isNotificationsOpen,
        onBack = shellState::closeNotifications,
    )
    PlatformSystemBackHandler(
        enabled = !shellState.isEditorOpen && !shellState.isDetailOpen && !shellState.isDiscoverOpen &&
            !shellState.isCalendarOpen && !shellState.isSeasonalOpen && !shellState.isNotificationsOpen && shellState.isAccountDetail,
        onBack = shellState::handleAccountBack,
    )
    val titleText = when {
        shellState.currentDetail is DetailDestination.Character -> strings.characterDetailTitle
        shellState.currentDetail is DetailDestination.Studio -> strings.studioDetailTitle
        shellState.currentDetail is DetailDestination.Staff -> strings.staffDetailTitle
        shellState.isDetailOpen -> strings.mediaDetailTitle
        shellState.isDiscoverOpen -> strings.discoverTitle
        shellState.isCalendarOpen -> strings.calendarTitle
        shellState.isSeasonalOpen -> strings.discoverSeasonalAction
        shellState.isNotificationsOpen -> strings.notificationsAction
        shellState.selectedTab == ShellTabSection.ACCOUNT &&
            shellState.accountRoute == ShellAccountRoute.SETTINGS -> strings.settingsAction
        shellState.selectedTab == ShellTabSection.ACCOUNT &&
            shellState.accountRoute == ShellAccountRoute.EDIT_PROFILE -> strings.accountEditProfileAction
        shellState.selectedTab == ShellTabSection.ACCOUNT &&
            shellState.accountRoute == ShellAccountRoute.SHARE_PROFILE -> strings.accountShareProfileAction
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
                        ShellTabSection.DISCOVER -> DashboardScreen(
                            stateHolder = dashboardState,
                            onOpenMedia = shellState::openMedia,
                            onEditMedia = shellState::openEditor,
                            onSeeAll = { rail ->
                                when (rail) {
                                    DashboardRail.THIS_SEASON -> {
                                        seasonalState.setSeasonYear(currentSeasonAndYear())
                                        shellState.openSeasonal()
                                    }
                                    DashboardRail.UPCOMING_NEXT_SEASON -> {
                                        seasonalState.setSeasonYear(currentSeasonAndYear().next())
                                        shellState.openSeasonal()
                                    }
                                    DashboardRail.TRENDING_ANIME ->
                                        openDiscoverWith(DiscoverCategory.ANIME, "TRENDING_DESC")
                                    DashboardRail.TRENDING_MANGA ->
                                        openDiscoverWith(DiscoverCategory.MANGA, "TRENDING_DESC")
                                    DashboardRail.ALL_TIME_POPULAR_ANIME ->
                                        openDiscoverWith(DiscoverCategory.ANIME, "POPULARITY_DESC")
                                    DashboardRail.ALL_TIME_POPULAR_MANGA ->
                                        openDiscoverWith(DiscoverCategory.MANGA, "POPULARITY_DESC")
                                    DashboardRail.TOP_ANIME ->
                                        openDiscoverWith(DiscoverCategory.ANIME, "SCORE_DESC")
                                    DashboardRail.TOP_MANGA ->
                                        openDiscoverWith(DiscoverCategory.MANGA, "SCORE_DESC")
                                }
                            },
                        )
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
                            accountStats = accountState.stats,
                            accountFavorites = accountState.favorites,
                            accountIsRefreshing = accountState.isRefreshing,
                            onAccountRefresh = { accountState.load(viewer.id, forceRefresh = true) },
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

            if (shellState.isDiscoverOpen) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    DiscoverScreen(
                        stateHolder = discoverState,
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

            if (shellState.isSeasonalOpen) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    SeasonalScreen(
                        stateHolder = seasonalState,
                        showSortSheet = showSeasonalSort,
                        onDismissSortSheet = { showSeasonalSort = false },
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

            if (!shellState.isAccountDetail && !shellState.isDetailOpen && !shellState.isDiscoverOpen && !shellState.isCalendarOpen && !shellState.isSeasonalOpen && !shellState.isNotificationsOpen) {
                ShellBottomCluster(
                    selectedTab = shellState.selectedTab,
                    onSearchClick = {
                        discoverState.enterBlankSearch()
                        shellState.openDiscover()
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
                isDiscoverOpen = shellState.isDiscoverOpen,
                isCalendarOpen = shellState.isCalendarOpen,
                isSeasonalOpen = shellState.isSeasonalOpen,
                isNotificationsOpen = shellState.isNotificationsOpen,
                titleText = titleText,
                onAccountBack = shellState::handleAccountBack,
                onMediaBack = shellState::closeDetail,
                onDiscoverBack = {
                    if (showDiscoverSortDropdown) {
                        showDiscoverSortDropdown = false
                    } else {
                        shellState.closeDiscover()
                    }
                },
                onCalendarBack = shellState::closeCalendar,
                onSeasonalBack = {
                    if (showSeasonalSort) {
                        showSeasonalSort = false
                    } else {
                        shellState.closeSeasonal()
                    }
                },
                onNotificationsBack = shellState::closeNotifications,
                onOpenSettings = { shellState.accountRoute = ShellAccountRoute.SETTINGS },
                onNotificationsClick = {
                    shellState.openNotifications()
                    notificationsState.load(resetCount = true)
                },
                onSortClick = { showListFilterSheet = true },
                onSectionFilterClick = shellState::openSectionFilter,
                onOpenCalendar = shellState::openCalendar,
                onOpenDiscoverSort = { showDiscoverSortDropdown = true },
                onDiscoverFilterClick = shellState::openDiscoverFilter,
                isDiscoverEntitySearch = discoverState.isEntitySearch(),
                sortDropdownExpanded = showDiscoverSortDropdown,
                selectedDiscoverSort = discoverState.sortSelection,
                onDiscoverSortSelect = {
                    discoverState.selectSort(it)
                    showDiscoverSortDropdown = false
                },
                onDiscoverSortDismiss = { showDiscoverSortDropdown = false },
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
            val sortOption = if (tab == ShellTabSection.ANIME) animeState.currentSortOption else mangaState.currentSortOption
            val sortOrder = if (tab == ShellTabSection.ANIME) animeState.currentSortOrder else mangaState.currentSortOrder
            val isPersisted = if (tab == ShellTabSection.ANIME) animeState.isFilterPersisted else mangaState.isFilterPersisted

            PlatformListFilterSheet(
                currentSort = sortOption,
                currentOrder = sortOrder,
                persist = isPersisted,
                isManga = tab == ShellTabSection.MANGA,
                onDismiss = { showListFilterSheet = false },
                onApply = { newSort, newOrder, newPersist ->
                    if (tab == ShellTabSection.ANIME) {
                        animeState.updateSort(newSort, newOrder, newPersist)
                    } else {
                        mangaState.updateSort(newSort, newOrder, newPersist)
                    }
                    filterSettings.saveFilter(newSort.name, newOrder.name, newPersist)
                    showListFilterSheet = false
                }
            )
        }

        if (shellState.isDiscoverFilterOpen) {
            val isSpanish = strings.languageLabel.equals("Idioma", ignoreCase = true)
            DiscoverFiltersSheet(
                groups = buildDiscoverFilterGroups(discoverState, isSpanish),
                onSelect = { groupId, optionId ->
                    discoverState.applyFilterSelection(groupId, optionId)
                },
                onDismiss = shellState::closeDiscoverFilter,
            )
        }

        if (shellState.isSectionFilterOpen) {
            val sectionOptions: List<ShellSectionOption>?
            val selectedSectionId: String?
            when (shellState.selectedTab) {
                ShellTabSection.ANIME -> {
                    sectionOptions = AnimeListSection.entries.map { section ->
                        ShellSectionOption(
                            id = section.name,
                            label = section.label(strings),
                            count = animeState.countInSection(section),
                        )
                    }
                    selectedSectionId = shellState.selectedAnimeSection.name
                }
                ShellTabSection.MANGA -> {
                    sectionOptions = MangaListSection.entries.map { section ->
                        ShellSectionOption(
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
                ShellSectionFilterSheet(
                    options = sectionOptions,
                    selectedId = selectedSectionId,
                    onSelect = { id ->
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
