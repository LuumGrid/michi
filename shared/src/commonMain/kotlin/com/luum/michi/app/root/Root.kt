package com.luum.michi.app.root

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
import com.luum.michi.app.mediaList.presentation.common.MediaListFilterSheet
import com.luum.michi.app.discover.domain.DashboardRepository
import com.luum.michi.app.discover.presentation.dashboard.DashboardRail
import com.luum.michi.app.discover.presentation.dashboard.DashboardScreen
import com.luum.michi.app.discover.presentation.dashboard.state.rememberDashboardStateHolder
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.presentation.explore.ExploreScreen
import com.luum.michi.app.discover.presentation.explore.ExploreFilterSheet
import com.luum.michi.app.discover.domain.model.ExploreCategory
import com.luum.michi.app.discover.presentation.explore.state.rememberExploreStateHolder
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.presentation.CalendarScreen
import com.luum.michi.app.calendar.presentation.state.rememberCalendarStateHolder
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.model.currentSeasonAndYear
import com.luum.michi.app.core.model.next
import com.luum.michi.app.ui.language.LanguageProvider
import com.luum.michi.app.ui.SystemBackHandler
import com.luum.michi.app.core.session.Viewer
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.core.anilist.medialist.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.presentation.media.MediaDetailScreen
import com.luum.michi.app.mediaDetail.presentation.media.components.MediaDetailEditorSheet
import com.luum.michi.app.mediaDetail.presentation.media.state.rememberMediaDetailStateHolder
import com.luum.michi.app.mediaDetail.presentation.media.state.rememberMediaEntryEditorState
import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.mediaDetail.presentation.character.CharacterDetailScreen
import com.luum.michi.app.mediaDetail.presentation.character.state.rememberCharacterDetailStateHolder
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.mediaDetail.presentation.staff.StaffDetailScreen
import com.luum.michi.app.mediaDetail.presentation.staff.state.rememberStaffDetailStateHolder
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import com.luum.michi.app.mediaDetail.presentation.studio.StudioDetailScreen
import com.luum.michi.app.mediaDetail.presentation.studio.state.rememberStudioDetailStateHolder
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
import com.luum.michi.app.root.components.AccountRouter
import com.luum.michi.app.ui.SettingsStoreKeys
import com.luum.michi.app.ui.components.FilterOption
import com.luum.michi.app.ui.rememberFilterSettings
import com.luum.michi.app.ui.rememberSettingsStore
import com.luum.michi.app.ui.components.SortSheet
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.root.components.BottomCluster
import com.luum.michi.app.root.components.TabSection
import com.luum.michi.app.root.components.label
import com.luum.michi.app.root.components.Toolbar
import com.luum.michi.app.root.state.DetailDestination
import com.luum.michi.app.root.state.AccountRoute
import com.luum.michi.app.root.state.rememberState

@Composable
internal fun Root(
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
    val settingsStore = rememberSettingsStore()
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
            ?.toTab() ?: TabSection.DISCOVER
    }
    val state = rememberState(viewer, initialTab)
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
        state.openExplore()
    }

    val filterSettings = rememberFilterSettings()
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

    LaunchedEffect(state.selectedTab) {
        when (state.selectedTab) {
            TabSection.ANIME -> {
                if (animeState.entries.isEmpty() && !animeState.isLoading) {
                    animeState.load(viewer.id)
                }
            }
            TabSection.MANGA -> {
                if (mangaState.entries.isEmpty() && !mangaState.isLoading) {
                    mangaState.load(viewer.id)
                }
            }
            TabSection.ACCOUNT -> {
                if (accountState.stats.animeCount == 0 && !accountState.isLoading) {
                    accountState.load(viewer.id)
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(state.accountRoute, state.favoritesCategory) {
        if (state.accountRoute == AccountRoute.FAVORITES) {
            favoritesGridState.load(viewer.id, state.favoritesCategory)
        }
    }

    SystemBackHandler(
        enabled = state.isEditorOpen,
        onBack = state::closeEditor,
    )
    SystemBackHandler(
        enabled = !state.isEditorOpen && state.isDetailOpen,
        onBack = state::closeDetail,
    )
    SystemBackHandler(
        enabled = !state.isEditorOpen && !state.isDetailOpen && state.isExploreOpen,
        onBack = {
            if (state.isExploreFilterOpen) {
                state.closeExploreFilter()
            } else {
                state.closeExplore()
            }
        },
    )
    SystemBackHandler(
        enabled = !state.isEditorOpen && !state.isDetailOpen && !state.isExploreOpen &&
                state.isCalendarOpen,
        onBack = state::closeCalendar,
    )
    SystemBackHandler(
        enabled = !state.isEditorOpen && !state.isDetailOpen && !state.isExploreOpen &&
                !state.isCalendarOpen && state.isNotificationsOpen,
        onBack = state::closeNotifications,
    )
    SystemBackHandler(
        enabled = !state.isEditorOpen && !state.isDetailOpen && !state.isExploreOpen &&
                !state.isCalendarOpen && !state.isNotificationsOpen && state.isAccountDetail,
        onBack = state::handleAccountBack,
    )
    val titleText = when {
        state.currentDetail is DetailDestination.Character -> strings.characterDetailTitle
        state.currentDetail is DetailDestination.Studio -> strings.studioDetailTitle
        state.currentDetail is DetailDestination.Staff -> strings.staffDetailTitle
        state.isDetailOpen -> strings.mediaDetailTitle
        state.isCalendarOpen -> strings.calendarTitle
        state.isNotificationsOpen -> strings.notificationsAction
        state.selectedTab == TabSection.ACCOUNT &&
            state.accountRoute == AccountRoute.SETTINGS -> strings.settingsAction
        state.selectedTab == TabSection.ACCOUNT &&
            state.accountRoute == AccountRoute.STATS -> strings.accountStatsTitle
        state.selectedTab == TabSection.ACCOUNT &&
                state.accountRoute == AccountRoute.FAVORITES -> when (state.favoritesCategory) {
            AccountFavoritesCategory.ANIME -> strings.accountFavoriteAnimeTitle
            AccountFavoritesCategory.MANGA -> strings.accountFavoriteMangaTitle
            AccountFavoritesCategory.CHARACTERS -> strings.accountFavoriteCharactersTitle
            AccountFavoritesCategory.STAFF -> strings.accountFavoriteStaffTitle
            AccountFavoritesCategory.STUDIOS -> strings.accountFavoriteStudiosTitle
        }
        state.selectedTab == TabSection.ACCOUNT ->
            state.currentProfile.username
        else -> state.selectedTab.label(strings)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
    ) {
        // Contenido full-bleed: la toolbar y el bottom cluster flotan por encima.
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = state.selectedTab,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                modifier = Modifier.fillMaxSize(),
                label = "tabCrossfade",
            ) { tab ->
                tabStateHolder.SaveableStateProvider(tab) {
                    when (tab) {
                        TabSection.DISCOVER -> Crossfade(
                            targetState = state.isExploreOpen,
                            label = "discoverExploreCrossfade",
                        ) { showingExplore ->
                            if (showingExplore) {
                                ExploreScreen(
                                    stateHolder = exploreState,
                                    onOpenMedia = state::openMedia,
                                    onEditMedia = state::openEditor,
                                )
                            } else {
                                DashboardScreen(
                                    stateHolder = dashboardState,
                                    onOpenMedia = state::openMedia,
                                    onEditMedia = state::openEditor,
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
                        TabSection.ANIME -> AnimeScreen(
                            stateHolder = animeState,
                            selectedSection = state.selectedAnimeSection,
                            onOpenMedia = state::openMedia,
                            onEditMedia = state::openEditor,
                            onCompletionReached = { id, progress ->
                                state.openEditorForCompletion(id, progress)
                            },
                            onRefresh = { animeState.load(viewer.id, forceRefresh = true) },
                        )
                        TabSection.MANGA -> MangaScreen(
                            stateHolder = mangaState,
                            selectedSection = state.selectedMangaSection,
                            onOpenMedia = state::openMedia,
                            onEditMedia = state::openEditor,
                            onCompletionReached = { id, progress ->
                                state.openEditorForCompletion(id, progress)
                            },
                            onRefresh = { mangaState.load(viewer.id, forceRefresh = true) },
                        )
                        TabSection.ACCOUNT -> AccountRouter(
                            route = state.accountRoute,
                            profile = state.currentProfile,
                            settingsState = settingsState,
                            accountState = accountState,
                            state = state,
                            favoritesCategory = state.favoritesCategory,
                            favoritesGridStateHolder = favoritesGridState,
                            language = language,
                            isDarkMode = isDarkMode,
                            onLanguageChange = onLanguageChange,
                            onToggleTheme = onToggleTheme,
                            onNavigate = { state.accountRoute = it },
                            onOpenAnimeList = { state.selectTab(TabSection.ANIME) },
                            onOpenMangaList = { state.selectTab(TabSection.MANGA) },
                            onOpenMedia = state::openMedia,
                            onEditMedia = state::openEditor,
                            onOpenCharacter = state::openCharacter,
                            onOpenStaff = state::openStaff,
                            onOpenStudio = state::openStudio,
                            onOpenFavoritesGrid = state::openFavoritesGrid,
                            onLogout = onLogout,
                            onBackHandlerChange = { state.toolbarBackHandler = it },
                        )
                    }
                }
            }

            if (state.isCalendarOpen) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    CalendarScreen(
                        stateHolder = calendarState,
                        onOpenMedia = state::openMedia,
                        onEditMedia = state::openEditor,
                    )
                }
            }

            if (state.isNotificationsOpen) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    NotificationsScreen(
                        stateHolder = notificationsState,
                        onOpen = { target ->
                            when (target) {
                                is NotificationTarget.Media -> {
                                    state.closeNotifications()
                                    state.openMedia(target.id)
                                }
                                is NotificationTarget.Web -> uriHandler.openUri(target.url)
                                NotificationTarget.None -> {}
                            }
                        },
                    )
                }
            }

            when (val dest = state.currentDetail) {
                is DetailDestination.Media -> detailStateHolder.SaveableStateProvider("media-${dest.id}") {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        MediaDetailScreen(
                            mediaId = dest.id,
                            stateHolder = mediaDetailState,
                            onRequestEdit = state::openEditor,
                            onOpenRelation = state::openMedia,
                            onOpenStudio = state::openStudio,
                            onOpenCharacter = state::openCharacter,
                            onOpenStaff = state::openStaff,
                        )
                    }
                }
                is DetailDestination.Studio -> detailStateHolder.SaveableStateProvider("studio-${dest.id}") {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        StudioDetailScreen(
                            id = dest.id,
                            stateHolder = studioDetailState,
                            onOpenMedia = state::openMedia,
                        )
                    }
                }
                is DetailDestination.Character -> detailStateHolder.SaveableStateProvider("character-${dest.id}") {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        CharacterDetailScreen(
                            id = dest.id,
                            stateHolder = characterDetailState,
                            onOpenMedia = state::openMedia,
                            onOpenStaff = state::openStaff,
                        )
                    }
                }
                is DetailDestination.Staff -> detailStateHolder.SaveableStateProvider("staff-${dest.id}") {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        StaffDetailScreen(
                            id = dest.id,
                            stateHolder = staffDetailState,
                            onOpenMedia = state::openMedia,
                            onOpenCharacter = state::openCharacter,
                        )
                    }
                }
                else -> {}
            }

            if (!state.isAccountDetail && !state.isDetailOpen && !state.isCalendarOpen && !state.isNotificationsOpen) {
                BottomCluster(
                    selectedTab = state.selectedTab,
                    onSearchClick = {
                        state.selectTab(TabSection.DISCOVER)
                        exploreState.enterBlankSearch()
                        state.openExplore()
                    },
                    onSelectTab = state::selectTab,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // Toolbar global flotante: overlay dibujado al final para quedar sobre el contenido.
            Toolbar(
                selectedTab = state.selectedTab,
                isAccountDetail = state.isAccountDetail,
                isDetailOpen = state.isDetailOpen,
                isExploreOpen = state.isExploreOpen,
                isCalendarOpen = state.isCalendarOpen,
                isNotificationsOpen = state.isNotificationsOpen,
                titleText = titleText,
                onAccountBack = state::handleAccountBack,
                onMediaBack = state::closeDetail,
                onExploreBack = {
                    if (state.isExploreFilterOpen) {
                        state.closeExploreFilter()
                    } else {
                        state.closeExplore()
                    }
                },
                onCalendarBack = state::closeCalendar,
                onNotificationsBack = state::closeNotifications,
                onOpenSettings = { state.accountRoute = AccountRoute.SETTINGS },
                onNotificationsClick = {
                    state.openNotifications()
                    notificationsState.load(resetCount = true)
                },
                onSortClick = { showListFilterSheet = true },
                onSectionFilterClick = {
                    if (state.selectedTab == TabSection.DISCOVER) {
                        state.openExploreFilter()
                    } else {
                        state.openSectionFilter()
                    }
                },
                onOpenCalendar = state::openCalendar,
                onOpenExplore = { openExploreWith(ExploreCategory.ANIME, UserListSort.TRENDING) },
                exploreQuery = exploreState.query,
                onExploreQueryChange = { exploreState.updateFilters(newQuery = it) },
                exploreFocusRequested = exploreState.focusSearchRequested,
                onExploreFocusConsumed = exploreState::consumeFocusRequest,
                onExploreFilterClick = state::openExploreFilter,
                unreadCount = notificationsState.unreadCount,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        state.editorMediaId?.let { editorMediaId ->
            val editorState = rememberMediaEntryEditorState(
                mediaId = editorMediaId,
                entryRepository = mediaListEntryRepository,
                detailRepository = mediaDetailRepository,
                initialStatusOverride = state.editorInitialStatus,
                initialProgressOverride = state.editorInitialProgress,
            )
            MediaDetailEditorSheet(
                state = editorState,
                onDismiss = state::closeEditor,
                onSaved = {
                    state.closeEditor()
                    // Only reload the list type that was actually edited to avoid
                    // firing 2 heavy queries when only 1 was needed.
                    // forceRefresh = true bypasses the TTL cache to reflect the edit.
                    if (editorState.isManga) {
                        mangaState.load(viewer.id, forceRefresh = true)
                    } else {
                        animeState.load(viewer.id, forceRefresh = true)
                    }
                    if (state.selectedMediaId == editorMediaId) {
                        mediaDetailState.refresh()
                    }
                },
                onDeleted = {
                    state.closeEditor()
                    if (editorState.isManga) {
                        mangaState.load(viewer.id, forceRefresh = true)
                    } else {
                        animeState.load(viewer.id, forceRefresh = true)
                    }
                    if (state.selectedMediaId == editorMediaId) {
                        mediaDetailState.refresh()
                    }
                },
            )
        }

        if (showListFilterSheet) {
            val tab = state.selectedTab
            val sortOption = when (tab) {
                TabSection.DISCOVER -> exploreState.currentSortOption
                TabSection.ANIME -> animeState.currentSortOption
                TabSection.MANGA -> mangaState.currentSortOption
                else -> animeState.currentSortOption
            }
            val sortOrder = when (tab) {
                TabSection.DISCOVER -> exploreState.currentSortOrder
                TabSection.ANIME -> animeState.currentSortOrder
                TabSection.MANGA -> mangaState.currentSortOrder
                else -> animeState.currentSortOrder
            }
            val isPersisted = when (tab) {
                TabSection.DISCOVER -> exploreState.isFilterPersisted
                TabSection.ANIME -> animeState.isFilterPersisted
                TabSection.MANGA -> mangaState.isFilterPersisted
                else -> false
            }

            SortSheet(
                currentSort = sortOption,
                currentOrder = sortOrder,
                persist = isPersisted,
                isManga = tab == TabSection.MANGA ||
                        (tab == TabSection.DISCOVER && exploreState.category == ExploreCategory.MANGA),
                onDismiss = { showListFilterSheet = false },
                onApply = { newSort, newOrder, newPersist ->
                    when (tab) {
                        TabSection.DISCOVER -> exploreState.updateSort(newSort, newOrder, newPersist)
                        TabSection.ANIME -> {
                            animeState.updateSort(newSort, newOrder, newPersist)
                            filterSettings.saveFilter(newSort.name, newOrder.name, newPersist)
                        }
                        TabSection.MANGA -> {
                            mangaState.updateSort(newSort, newOrder, newPersist)
                            filterSettings.saveFilter(newSort.name, newOrder.name, newPersist)
                        }
                        else -> {}
                    }
                    showListFilterSheet = false
                }
            )
        }

        if (state.isExploreFilterOpen) {
            ExploreFilterSheet(
                stateHolder = exploreState,
                onDismiss = state::closeExploreFilter,
            )
        }

        if (state.isAccountSettingsOpen) {
            AccountSettingsSheet(onDismiss = state::closeAccountSettings)
        }

        if (state.isShareProfileOpen) {
            AccountShareProfileSheet(
                username = state.currentProfile.username,
                displayName = state.currentProfile.displayName,
                avatarUrl = state.currentProfile.avatarUrl,
                onDismiss = state::closeShareProfile,
            )
        }

        if (state.isSectionFilterOpen) {
            when (state.selectedTab) {
                TabSection.ANIME -> {
                    MediaListFilterSheet(
                        sectionOptions = AnimeListSection.entries.map { section ->
                            FilterOption(
                                id = section.name,
                                label = section.label(strings),
                                count = animeState.countInSection(section),
                            )
                        },
                        initialSectionId = state.selectedAnimeSection.name,
                        showSeasonFilter = true,
                        isAnimeTab = true,
                        season = animeState.filterSeason,
                        genres = animeState.filterGenres,
                        formats = animeState.filterFormats,
                        year = animeState.filterYear,
                        onApply = { sectionId, season, genres, formats, year ->
                            AnimeListSection.entries
                                .firstOrNull { it.name == sectionId }
                                ?.let { state.selectedAnimeSection = it }
                            animeState.updateListFilters(season, genres, formats, year)
                        },
                        onDismiss = state::closeSectionFilter,
                    )
                }
                TabSection.MANGA -> {
                    MediaListFilterSheet(
                        sectionOptions = MangaListSection.entries.map { section ->
                            FilterOption(
                                id = section.name,
                                label = section.label(strings),
                                count = mangaState.countInSection(section),
                            )
                        },
                        initialSectionId = state.selectedMangaSection.name,
                        showSeasonFilter = false,
                        isAnimeTab = false,
                        season = mangaState.filterSeason,
                        genres = mangaState.filterGenres,
                        formats = mangaState.filterFormats,
                        year = mangaState.filterYear,
                        onApply = { sectionId, season, genres, formats, year ->
                            MangaListSection.entries
                                .firstOrNull { it.name == sectionId }
                                ?.let { state.selectedMangaSection = it }
                            mangaState.updateListFilters(season, genres, formats, year)
                        },
                        onDismiss = state::closeSectionFilter,
                    )
                }
                else -> {}
            }
        }
    }
}

/** Maps the persisted "default discover tab" preference onto the root tab-bar sections. */
private fun DiscoverTabOption.toTab(): TabSection = when (this) {
    DiscoverTabOption.DISCOVER -> TabSection.DISCOVER
    DiscoverTabOption.ANIME -> TabSection.ANIME
    DiscoverTabOption.MANGA -> TabSection.MANGA
    DiscoverTabOption.ACCOUNT -> TabSection.ACCOUNT
}