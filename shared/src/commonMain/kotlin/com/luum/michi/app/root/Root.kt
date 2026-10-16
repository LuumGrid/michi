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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.ui.CalendarScreen
import com.luum.michi.app.calendar.ui.state.createCalendarStateHolder
import com.luum.michi.app.discover.domain.DashboardFeedCache
import com.luum.michi.app.discover.domain.DashboardRepository
import com.luum.michi.app.discover.ui.dashboard.DashboardScreen
import com.luum.michi.app.discover.ui.dashboard.state.createDashboardStateHolder
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.model.UserListOrder
import com.luum.michi.app.core.model.UserListSort
import com.luum.michi.app.core.navigation.domain.TabSection
import com.luum.michi.app.core.navigation.domain.label
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.core.storage.domain.SortPersistence
import com.luum.michi.app.core.storage.domain.SortScope
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.domain.genre.model.genreAniListUrl
import com.luum.michi.app.discover.domain.genre.model.genreShareText
import com.luum.michi.app.mediaDetail.domain.media.model.MediaDetailType
import com.luum.michi.app.mediaDetail.domain.media.model.aniListUrl
import com.luum.michi.app.mediaDetail.domain.media.model.shareText
import com.luum.michi.app.mediaDetail.domain.character.model.aniListUrl as characterAniListUrl
import com.luum.michi.app.mediaDetail.domain.character.model.shareText as characterShareText
import com.luum.michi.app.mediaDetail.domain.staff.model.aniListUrl as staffAniListUrl
import com.luum.michi.app.mediaDetail.domain.staff.model.shareText as staffShareText
import com.luum.michi.app.mediaDetail.domain.studio.model.aniListUrl as studioAniListUrl
import com.luum.michi.app.mediaDetail.domain.studio.model.shareText as studioShareText
import com.luum.michi.app.mediaDetail.ui.components.DetailListLayout
import com.luum.michi.app.mediaDetail.ui.components.DetailListScreen
import com.luum.michi.app.mediaDetail.ui.components.toRailItem
import com.luum.michi.app.mediaDetail.ui.character.CharacterDetailScreen
import com.luum.michi.app.mediaDetail.ui.character.state.createCharacterDetailStateHolder
import com.luum.michi.app.mediaDetail.ui.media.MediaDetailScreen
import com.luum.michi.app.mediaDetail.ui.media.MediaEntryEditorSheet
import com.luum.michi.app.mediaDetail.ui.media.state.createMediaDetailStateHolder
import com.luum.michi.app.mediaDetail.ui.media.state.createMediaEntryEditorState
import com.luum.michi.app.mediaDetail.ui.staff.StaffDetailScreen
import com.luum.michi.app.mediaDetail.ui.staff.state.createStaffDetailStateHolder
import com.luum.michi.app.mediaDetail.ui.studio.StudioDetailScreen
import com.luum.michi.app.ui.components.DetailGridSortOptions
import com.luum.michi.app.mediaDetail.ui.studio.state.createStudioDetailStateHolder
import com.luum.michi.app.discover.ui.genre.GenreDetailScreen
import com.luum.michi.app.discover.ui.genre.state.createGenreDetailStateHolder
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.DetailHolder
import com.luum.michi.app.core.navigation.domain.DetailListKind
import com.luum.michi.app.core.navigation.domain.GridSortHolder
import com.luum.michi.app.core.navigation.domain.ShareLauncher
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.settings.domain.model.ListSort
import com.luum.michi.app.settings.domain.model.ScoreFormat
import com.luum.michi.app.settings.domain.model.formatScoreValue
import com.luum.michi.app.settings.domain.model.parseScoreValue
import com.luum.michi.app.settings.domain.model.scoreMaxFor
import com.luum.michi.app.settings.domain.model.scoreStepFor
import com.luum.michi.app.settings.domain.model.scoreSuffixFor
import com.luum.michi.app.settings.domain.model.toUserSort
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.navigation.domain.UrlOpener
import com.luum.michi.app.mediaList.ui.anime.AnimeListScreen
import com.luum.michi.app.mediaList.ui.anime.state.AnimeListStateHolder
import com.luum.michi.app.mediaList.ui.manga.MangaListScreen
import com.luum.michi.app.mediaList.ui.manga.state.MangaListStateHolder
import com.luum.michi.app.root.state.State
import com.luum.michi.app.mediaList.ui.common.state.MediaListHolder
import com.luum.michi.app.root.state.resolveStartDestination
import com.luum.michi.app.root.state.startDestinationChoice
import com.luum.michi.app.root.state.toDashboardMediaType
import com.luum.michi.app.settings.domain.model.StartCatalogOption
import com.luum.michi.app.settings.domain.model.StartTabOption
import com.luum.michi.app.root.state.detailSurfaceKey
import com.luum.michi.app.root.state.detailSurfaceKeys
import com.luum.michi.app.root.state.rememberState
import com.luum.michi.app.settings.ui.SettingsScreen
import com.luum.michi.app.settings.ui.state.SettingsState
import com.luum.michi.app.ui.components.BottomEdgeFade
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.OptionRow
import com.luum.michi.app.ui.components.SearchScrim
import com.luum.michi.app.ui.components.SortSheet
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
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.ThemeColors
import com.luum.michi.app.ui.theme.ThemeType
import com.materialkolor.PaletteStyle
import com.luum.michi.app.ui.components.PlatformBackHandler
import com.luum.michi.app.root.state.BackStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    viewer: Viewer? = null,
    // The "Pestaña al abrir" setting seeds the shell state once (startup only):
    // a later settings change never moves the user. A guest can only land on
    // DISCOVER (the other tabs need a session) and a section id that no longer
    // exists (split-completed off) falls back to ALL.
    state: State = rememberState(
        startDestination = resolveStartDestination(
            startTab = settingsState.startTab,
            startCatalogType = settingsState.startCatalogType,
            animeSectionId = settingsState.startAnimeSectionId,
            mangaSectionId = settingsState.startMangaSectionId,
            splitCompletedAnime = settingsState.splitCompletedAnime,
            splitCompletedManga = settingsState.splitCompletedManga,
            isGuest = viewer == null,
        ),
    ),
    strings: LanguageStrings = Strings.current,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    palette: ThemeColors,
    onPaletteChange: (ThemeColors) -> Unit,
    themeStyle: PaletteStyle,
    onThemeStyleChange: (PaletteStyle) -> Unit,
    themeType: ThemeType,
    onThemeTypeChange: (ThemeType) -> Unit,
    font: AppFont,
    onFontChange: (AppFont) -> Unit,
    surfaces: SurfaceStyle,
    onSurfacesChange: (SurfaceStyle) -> Unit,
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

    // System back funnels through the same progressive step as the toolbar
    // chevron. When nothing is left to consume (SystemDefault) the handler is
    // disabled, so the platform closes the app as usual.
    PlatformBackHandler(enabled = state.nextBackStep() != BackStep.SystemDefault) {
        state.applyBackStep()
    }

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

    // ── Session transitions ────────────────────────────────────────────────
    // viewerId drives three things: seeding/clearing the synced prefs, resetting
    // the transient navigation state, and re-keying the holders (their favourite
    // state is per-user). It only fires on a real change, so a cold start (or a
    // deep link opened during it) never loses navigation.
    val viewerId = viewer?.id
    // Latest strings for the holders' providers, without keying them on language.
    val currentStrings by rememberUpdatedState(strings)

    // Anime list holder, recreated per viewer. Loads are explicit and the
    // loader dedups repeats via TTL; guests never load (no user id).
    val listScope = rememberCoroutineScope()

    // Airing calendar holder: session-independent (public feed, no viewer id)
    // and shared by guests. Declared before the list holders so their
    // `onEntrySaved` can invalidate its cache.
    val calendarScope = rememberHolderScope(listScope)
    val calendarHolder = remember(calendarRepository, calendarScope) {
        createCalendarStateHolder(calendarRepository, calendarScope)
    }

    // Session change: drop the transient navigation state (the synced prefs are
    // seeded/cleared in App, next to the settings holder) and mark the calendar
    // stale — its rows carry the viewer's status/score.
    var lastViewerId by remember { mutableStateOf(viewerId) }
    LaunchedEffect(viewerId) {
        if (viewerId != lastViewerId) {
            state.reset()
            calendarHolder.invalidateEntries()
            lastViewerId = viewerId
        }
    }

    val animeScope = rememberHolderScope(listScope, viewerId)
    val mangaScope = rememberHolderScope(listScope, viewerId)
    val animeHolder = remember(viewerId, animeListRepository, mediaListEntryRepository, sortPersistence, animeScope) {
        viewerId?.let {
            AnimeListStateHolder(
                animeListRepository,
                mediaListEntryRepository,
                animeScope,
                sortPersistence,
                onEntrySaved = { calendarHolder.invalidateEntries() },
            )
        }
    }
    val mangaHolder = remember(viewerId, mangaListRepository, mediaListEntryRepository, sortPersistence, mangaScope) {
        viewerId?.let {
            MangaListStateHolder(
                mangaListRepository,
                mediaListEntryRepository,
                mangaScope,
                sortPersistence,
                onEntrySaved = { calendarHolder.invalidateEntries() },
            )
        }
    }
    // Server-mapped titles (userPreferred) and person names only re-map on load:
    // a confirmed language change (syncedGeneration) forces one reload so cards
    // stop showing stale titles. A list that is not visible reloads when it
    // opens, via its pending flag.
    var lastSyncedGeneration by remember { mutableStateOf(settingsState.syncedGeneration) }
    var pendingAnimeReload by remember { mutableStateOf(false) }
    var pendingMangaReload by remember { mutableStateOf(false) }
    LaunchedEffect(
        tab,
        viewerId,
        settingsState.splitCompletedAnime,
        settingsState.splitCompletedManga,
        settingsState.syncedGeneration,
    ) {
        val languageSynced = settingsState.syncedGeneration != lastSyncedGeneration
        if (languageSynced) {
            pendingAnimeReload = true
            pendingMangaReload = true
        }
        // Always load the visible list (the TTL dedups repeats): opening the tab,
        // a new viewer or a split flip must fetch, not only a language change.
        if (tab == TabSection.ANIME && viewerId != null) {
            animeHolder?.load(
                viewerId,
                forceRefresh = languageSynced || pendingAnimeReload,
                splitCompleted = settingsState.splitCompletedAnime,
            )
            pendingAnimeReload = false
        }
        if (tab == TabSection.MANGA && viewerId != null) {
            mangaHolder?.load(
                viewerId,
                forceRefresh = languageSynced || pendingMangaReload,
                splitCompleted = settingsState.splitCompletedManga,
            )
            pendingMangaReload = false
        }
        lastSyncedGeneration = settingsState.syncedGeneration
    }

    // Discover dashboard holder: session-independent (public catalog) and
    // shared by guests. Loads are explicit from the screen; the TTL dedups
    // revisits and the 18+ gate lives in the holder. The holder is keyed
    // without `strings` so a language change keeps the Anime/Manga selection;
    // the provider is read at launch so one request never mixes languages.
    val dashboardScope = rememberHolderScope(listScope)
    val dashboardHolder = remember(dashboardRepository, dashboardFeedCache, dashboardScope) {
        createDashboardStateHolder(
            repository = dashboardRepository,
            cache = dashboardFeedCache,
            scope = dashboardScope,
            hideAdult = !settingsState.displayAdultContent,
            // "Pestaña al abrir" → DISCOVER sub-choice; startup only, no load.
            initialType = settingsState.startCatalogType.toDashboardMediaType(),
        ) { currentStrings }
    }

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
    val onStartTabChange: (StartTabOption) -> Unit = { settingsState.startTab = it }
    val onStartSectionChange: (String) -> Unit = { value ->
        when (settingsState.startTab) {
            StartTabOption.DISCOVER -> StartCatalogOption.entries.firstOrNull { it.name == value }
                ?.let { settingsState.startCatalogType = it }
            StartTabOption.ANIME -> settingsState.startAnimeSectionId = value
            StartTabOption.MANGA -> settingsState.startMangaSectionId = value
            StartTabOption.PROFILE -> Unit
        }
    }

    // Per-surface saveable state (scroll positions): Root owns the holder so a
    // tab, each detail entry and the overlays keep their own position. A popped
    // detail entry drops its state (a new entry always starts at the top).
    val surfaceStates = rememberSaveableStateHolder()
    val detailEntryIds = state.detailEntryIds.toList()
    var lastDetailEntryIds by remember { mutableStateOf(detailEntryIds) }
    LaunchedEffect(detailEntryIds) {
        val gone = lastDetailEntryIds.toSet() - detailEntryIds.toSet()
        // An entry is saved under the tab of the slot that composed it, which
        // may no longer be the selected tab (a deep link or a tab change), so
        // drop every tab variant instead of guessing.
        gone.forEach { id -> detailSurfaceKeys(id).forEach(surfaceStates::removeState) }
        lastDetailEntryIds = detailEntryIds
    }
    // The cached rail meta is localized: a language change drops disk + memory
    // and bumps the holder's generation, which reloads the screen only when it
    // is visible (the screen's effect keys on the generation).
    var lastDashboardLanguage by remember { mutableStateOf(language) }
    LaunchedEffect(language) {
        if (language != lastDashboardLanguage) {
            lastDashboardLanguage = language
            dashboardHolder.invalidate()
        }
    }

    // Detail holders: session-independent data but per-user favourite state, so
    // they are re-keyed per viewer and read strings through a provider (a
    // language change keeps the holder and its cache). Each owns a child scope
    // cancelled when it is replaced.
    val mediaDetailScope = rememberHolderScope(listScope, viewerId)
    val mediaDetailHolder = remember(mediaDetailRepository, mediaListEntryRepository, mediaDetailScope, viewerId) {
        createMediaDetailStateHolder(
            mediaDetailRepository,
            mediaListEntryRepository,
            mediaDetailScope,
        ) { currentStrings }
    }
    val characterScope = rememberHolderScope(listScope, viewerId)
    val characterHolder = remember(characterDetailRepository, characterScope, viewerId) {
        createCharacterDetailStateHolder(characterDetailRepository, characterScope) { currentStrings }
    }
    val staffScope = rememberHolderScope(listScope, viewerId)
    val staffHolder = remember(staffDetailRepository, staffScope, viewerId) {
        createStaffDetailStateHolder(staffDetailRepository, staffScope) { currentStrings }
    }
    val studioScope = rememberHolderScope(listScope, viewerId)
    val studioHolder = remember(studioDetailRepository, studioScope, viewerId) {
        createStudioDetailStateHolder(studioDetailRepository, studioScope)
    }
    val genreScope = rememberHolderScope(listScope, viewerId)
    val genreHolder = remember(exploreRepository, genreScope, viewerId) {
        createGenreDetailStateHolder(
            exploreRepository,
            genreScope,
            !settingsState.displayAdultContent,
        )
    }
    // Every pushed detail page lives behind one contract, so refresh, cache
    // invalidation and the favourite action are a single lookup instead of a
    // five-branch `when` per call site.
    val detailHolders: List<DetailHolder> = listOf(
        mediaDetailHolder,
        characterHolder,
        staffHolder,
        studioHolder,
        genreHolder,
    )
    fun detailHolderFor(destination: DetailDestination?): DetailHolder? = when (destination) {
        is DetailDestination.Media -> mediaDetailHolder
        is DetailDestination.Character -> characterHolder
        is DetailDestination.Staff -> staffHolder
        is DetailDestination.Studio -> studioHolder
        is DetailDestination.Genre -> genreHolder
        is DetailDestination.DetailList -> null
        null -> null
    }
    // Only the server-ordered grids (studio, genre) open the detail sort sheet.
    fun gridSortHolderFor(destination: DetailDestination?): GridSortHolder? = when (destination) {
        is DetailDestination.Studio -> studioHolder
        is DetailDestination.Genre -> genreHolder
        else -> null
    }
    // 18+ gate: flipping it re-queries the open genre page (a no-op while no
    // genre is loaded), so the setting applies without leaving the page.
    LaunchedEffect(settingsState.displayAdultContent) {
        genreHolder.setHideAdult(!settingsState.displayAdultContent)
    }
    // A confirmed language change (titles/staff names) also re-localizes the
    // open detail page and the calendar day; the dashboard re-hydrates when its
    // screen is visible again (invalidate bumps its generation).
    LaunchedEffect(settingsState.syncedGeneration) {
        if (settingsState.syncedGeneration == 0) return@LaunchedEffect
        dashboardHolder.invalidate()
        // Every cached detail snapshot is re-localized: drop all of them so the
        // back stack never restores stale titles, then refresh the visible one.
        detailHolders.forEach { it.invalidateCache() }
        detailHolderFor(state.currentDetail)?.refresh()
        // Always: the calendar rows are re-localized too (open or not).
        calendarHolder.invalidateEntries()
    }
    LaunchedEffect(state.currentDetail) {
        when (val destination = state.currentDetail) {
            is DetailDestination.Media -> mediaDetailHolder.load(destination.id)
            is DetailDestination.Character -> characterHolder.load(destination.id)
            is DetailDestination.Staff -> staffHolder.load(destination.id)
            is DetailDestination.Studio -> studioHolder.load(destination.id)
            is DetailDestination.Genre -> genreHolder.load(destination.name, destination.type)
            // The list renders the parent's holder. Re-bind that holder to
            // [parentId] because an item opened from the list may have swapped
            // it for another media/character/staff; `load` is a no-op when the
            // parent is already current (or restores its snapshot from cache).
            is DetailDestination.DetailList -> when (destination.kind) {
                DetailListKind.MEDIA_CHARACTERS,
                DetailListKind.MEDIA_STAFF,
                DetailListKind.MEDIA_RELATED,
                DetailListKind.MEDIA_RECOMMENDATIONS -> {
                    mediaDetailHolder.load(destination.parentId)
                    // Recommendations are a per-visit query, not cached: refetch
                    // the parent's page 1 so the "View all" list is not empty.
                    if (destination.kind == DetailListKind.MEDIA_RECOMMENDATIONS) {
                        mediaDetailHolder.loadRecommendations()
                    }
                }
                DetailListKind.CHARACTER_MEDIA -> characterHolder.load(destination.parentId)
                DetailListKind.STAFF_MEDIA,
                DetailListKind.STAFF_CHARACTERS -> staffHolder.load(destination.parentId)
            }
            null -> {}
        }
    }

    // Deep link (anilist.co/anime|manga/{id}): open the media detail once the
    // shell composes, then clear the pending id so it never reopens.
    LaunchedEffect(deepLinkMediaId) {
        deepLinkMediaId?.let {
            state.openMedia(it)
            onDeepLinkHandled()
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
        val sortTargets: List<Pair<SortScope, MediaListHolder<*, *>?>> = listOf(
            SortScope.ANIME to animeHolder,
            SortScope.MANGA to mangaHolder,
        )
        sortTargets.forEach { (scope, holder) ->
            val initial = resolveInitialSort(
                saved = hydrateSort(sortPersistence, scope).takeIf { persistSort },
                defaultSort = defaultSort,
            )
            holder?.updateSort(initial.option, initial.order, persist = initial.persist)
        }
    }

    // Editor save/delete land here: both lists re-fetch so the edited
    // entry (possibly moved sections) shows the truth. Rare action,
    // two queries, zero staleness.
    fun refreshLists() {
        val id = viewerId ?: return
        animeHolder?.load(id, forceRefresh = true, splitCompleted = settingsState.splitCompletedAnime)
        mangaHolder?.load(id, forceRefresh = true, splitCompleted = settingsState.splitCompletedManga)
        // A saved/edited entry changes the calendar's user status/score.
        calendarHolder.invalidateEntries()
    }

    // Editor score display in the user's format (translation lives in
    // settings pure functions so mediaDetail stays primitive-only).
    val editorScoreFormat = settingsState.scoreFormat
    val formatScore: (Float) -> String = { formatScoreValue(editorScoreFormat, it) }
    val parseScore: (String) -> Float? = { parseScoreValue(editorScoreFormat, it) }
    val scoreSuffix: String? = scoreSuffixFor(editorScoreFormat)
    val scoreMax: Float = scoreMaxFor(editorScoreFormat)
    val scoreStep: Float = scoreStepFor(editorScoreFormat)
    // Card smiley pill: translated here so settings types never cross
    // into mediaList (same rule as the editor score lambdas).
    val isSmileyFormat: Boolean = editorScoreFormat == ScoreFormat.POINT_3_SMILEYS
    val formatSmiley: (Float) -> String = { formatScoreValue(ScoreFormat.POINT_3_SMILEYS, it) }

    // Pinned by default: the toolbar only hides on scroll for surfaces that
    // explicitly opt in via [hidesToolbarOnScroll]. The veil rides the same
    // offset so it stays glued to the toolbar when collapsing.
    val toolbarScroll = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val hideToolbarOnScroll = hidesToolbarOnScroll(tab, state)
    // The heart reads the holder that owns the active destination (media keeps
    // its flag on the detail snapshot; a genre has no favourite).
    val isDetailFavourite = detailHolderFor(state.currentDetail)?.isFavourite == true
    // Overflow menu target per active destination: title + canonical AniList
    // URL + share body. Null while the detail has not hydrated yet.
    val detailOverflow: DetailOverflow? = when (val destination = state.currentDetail) {
        is DetailDestination.Media -> mediaDetailHolder.detail?.let {
            DetailOverflow(it.title, it.aniListUrl(), it.shareText())
        }
        is DetailDestination.Character -> characterHolder.detail?.let {
            DetailOverflow(it.name, it.characterAniListUrl(), it.characterShareText())
        }
        is DetailDestination.Staff -> staffHolder.detail?.let {
            DetailOverflow(it.name, it.staffAniListUrl(), it.staffShareText())
        }
        is DetailDestination.Studio -> studioHolder.detail?.let {
            DetailOverflow(it.name, it.studioAniListUrl(), it.studioShareText())
        }
        // A genre is not an entity: the closest canonical page is the catalog
        // search filtered by genre, so it is ready before anything hydrates. The
        // URL follows the catalog chip the user has selected.
        is DetailDestination.Genre -> DetailOverflow(
            destination.name,
            genreAniListUrl(destination.name, genreHolder.type),
            genreShareText(destination.name, genreHolder.type),
        )
        is DetailDestination.DetailList -> null
        null -> null
    }
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
                title = detailToolbarTitle(state.currentDetail, strings)
                    ?: if (state.isDetailOpen) "" else toolbarTitle(state, tab, strings),
                navigation = if (search != null || state.isDetailOpen || state.isSettingsOpen || state.isCalendarOpen) {
                    ToolbarNavigation.Back(strings.backButton)
                } else {
                    ToolbarNavigation.None
                },
                actions = toolbarActions(
                    tab = tab,
                    isSearching = search != null,
                    isSettingsOpen = state.isSettingsOpen,
                    isCalendarOpen = state.isCalendarOpen,
                    isDetailOpen = state.isDetailOpen,
                    isMediaDetail = state.currentDetail is DetailDestination.Media,
                    isStudioDetail = state.currentDetail is DetailDestination.Studio,
                    isGenreDetail = state.currentDetail is DetailDestination.Genre,
                    isDetailList = state.currentDetail is DetailDestination.DetailList,
                    isDetailFavourite = isDetailFavourite,
                    isGuest = viewerId == null,
                    strings = strings,
                ),
                search = search,
                backContentDescription = strings.backButton,
                clearContentDescription = strings.clearSearchAction,
                onNavigation = { state.applyBackStep() },
                onAction = { id ->
                    when (id) {
                        ACTION_SEARCH -> state.openSearch(tab)
                        ACTION_SETTINGS -> state.openSettings()
                        // List tools only exist on ANIME/MANGA (see
                        // toolbarActions), so ACTION_FILTER always targets a
                        // list tab. ACTION_SORT also belongs to the studio
                        // detail page while it is open.
                        ACTION_FILTER -> state.openSectionFilter()
                        ACTION_SORT -> when (state.currentDetail) {
                            // The studio and genre grids are ordered by the
                            // server, so the sheet targets their holder.
                            is DetailDestination.Studio,
                            is DetailDestination.Genre -> state.openDetailSort()
                            else -> state.openListSort()
                        }
                        ACTION_CALENDAR -> state.openCalendar()
                        ACTION_CALENDAR_FILTER -> state.openCalendarFilter()
                        ACTION_EDIT -> mediaDetailHolder.detail?.id?.let { state.openEditor(it) }
                        ACTION_FAVOURITE -> detailHolderFor(state.currentDetail)?.toggleFavourite()
                        ACTION_MORE -> state.openDetailMenu()
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
                                language = language,
                                onLanguageChange = onLanguageChange,
                                palette = palette,
                                onPaletteChange = onPaletteChange,
                                themeStyle = themeStyle,
                                onThemeStyleChange = onThemeStyleChange,
                                themeType = themeType,
                                onThemeTypeChange = onThemeTypeChange,
                                font = font,
                                onFontChange = onFontChange,
                                surfaces = surfaces,
                                onSurfacesChange = onSurfacesChange,
                                startDestination = startChoice,
                                onStartTabChange = onStartTabChange,
                                onStartSectionChange = onStartSectionChange,
                                viewer = viewer,
                                onLogin = { state.openSignIn() },
                                onLogout = onLogout,
                            )
                        } else if (state.isDetailOpen) {
                            // Detail page over the current tab (tab bar hidden):
                            // pushes onto detailStack, back pops one at a time.
                            val destination = state.currentDetail
                            when (destination) {
                                is DetailDestination.Media -> MediaDetailScreen(
                                    holder = mediaDetailHolder,
                                    strings = strings,
                                    bottomPadding = padding.calculateBottomPadding(),
                                    onRetry = {
                                        if (mediaDetailHolder.detail == null) {
                                            mediaDetailHolder.load(destination.id)
                                        } else {
                                            mediaDetailHolder.refresh()
                                        }
                                    },
                                    onOpenMedia = { state.openMedia(it) },
                                    onOpenCharacter = { state.openCharacter(it) },
                                    onOpenStaff = { state.openStaff(it) },
                                    onOpenStudio = { state.openStudio(it) },
                                    onOpenGenre = { name, type -> state.openGenre(name, type) },
                                    onOpenUrl = { urlOpener.open(it) },
                                    onViewAll = { state.openDetailList(it, destination.id) },
                                )
                                is DetailDestination.Character -> CharacterDetailScreen(
                                    holder = characterHolder,
                                    strings = strings,
                                    bottomPadding = padding.calculateBottomPadding(),
                                    onRetry = {
                                        if (characterHolder.detail == null) {
                                            characterHolder.load(destination.id)
                                        } else {
                                            characterHolder.refresh()
                                        }
                                    },
                                    onOpenUrl = { urlOpener.open(it) },
                                    onOpenMedia = { state.openMedia(it) },
                                    onOpenStaff = { state.openStaff(it) },
                                    onViewAll = { state.openDetailList(it, destination.id) },
                                )
                                is DetailDestination.Staff -> StaffDetailScreen(
                                    holder = staffHolder,
                                    strings = strings,
                                    bottomPadding = padding.calculateBottomPadding(),
                                    onRetry = {
                                        if (staffHolder.detail == null) {
                                            staffHolder.load(destination.id)
                                        } else {
                                            staffHolder.refresh()
                                        }
                                    },
                                    onOpenUrl = { urlOpener.open(it) },
                                    onOpenMedia = { state.openMedia(it) },
                                    onOpenCharacter = { state.openCharacter(it) },
                                    onViewAll = { state.openDetailList(it, destination.id) },
                                )
                                is DetailDestination.Studio -> StudioDetailScreen(
                                    holder = studioHolder,
                                    strings = strings,
                                    bottomPadding = padding.calculateBottomPadding(),
                                    onRetry = {
                                        if (studioHolder.detail == null) {
                                            studioHolder.load(destination.id)
                                        } else {
                                            studioHolder.refresh()
                                        }
                                    },
                                    onOpenMedia = { state.openMedia(it) },
                                )
                                is DetailDestination.Genre -> GenreDetailScreen(
                                    holder = genreHolder,
                                    strings = strings,
                                    bottomPadding = padding.calculateBottomPadding(),
                                    onRetry = {
                                        if (genreHolder.pageLoaded) {
                                            genreHolder.refresh()
                                        } else {
                                            genreHolder.load(destination.name, destination.type)
                                        }
                                    },
                                    onOpenMedia = { state.openMedia(it) },
                                )
                                is DetailDestination.DetailList -> {
                                    val listBottom = padding.calculateBottomPadding()
                                    val isManga = mediaDetailHolder.detail?.type == MediaDetailType.MANGA
                                    val mediaError = mediaDetailHolder.actionError?.let { strings.networkErrorMessage(it) }
                                    val characterError = characterHolder.actionError?.let { strings.networkErrorMessage(it) }
                                    val staffError = staffHolder.actionError?.let { strings.networkErrorMessage(it) }
                                    when (destination.kind) {
                                        DetailListKind.MEDIA_CHARACTERS -> DetailListScreen(
                                            items = mediaDetailHolder.characters.items.map { it.toRailItem(strings) },
                                            layout = DetailListLayout.GRID,
                                            onSelect = { state.openCharacter(it) },
                                            bottomPadding = listBottom,
                                            hasMore = mediaDetailHolder.characters.hasNextPage,
                                            isLoadingMore = mediaDetailHolder.characters.isLoadingMore,
                                            onLoadMore = { mediaDetailHolder.loadMoreCharacters() },
                                            actionError = mediaError,
                                            dismissLabel = strings.dismissAction,
                                            onDismissActionError = { mediaDetailHolder.clearActionError() },
                                        )
                                        DetailListKind.MEDIA_STAFF -> DetailListScreen(
                                            items = mediaDetailHolder.staff.items.map { it.toRailItem() },
                                            layout = DetailListLayout.GRID,
                                            onSelect = { state.openStaff(it) },
                                            bottomPadding = listBottom,
                                            hasMore = mediaDetailHolder.staff.hasNextPage,
                                            isLoadingMore = mediaDetailHolder.staff.isLoadingMore,
                                            onLoadMore = { mediaDetailHolder.loadMoreStaff() },
                                            actionError = mediaError,
                                            dismissLabel = strings.dismissAction,
                                            onDismissActionError = { mediaDetailHolder.clearActionError() },
                                        )
                                        DetailListKind.MEDIA_RELATED -> DetailListScreen(
                                            items = mediaDetailHolder.detail?.relations.orEmpty().map { it.toRailItem(strings, isManga) },
                                            layout = DetailListLayout.ROWS,
                                            onSelect = { state.openMedia(it) },
                                            bottomPadding = listBottom,
                                            actionError = mediaError,
                                            dismissLabel = strings.dismissAction,
                                            onDismissActionError = { mediaDetailHolder.clearActionError() },
                                        )
                                        DetailListKind.MEDIA_RECOMMENDATIONS -> DetailListScreen(
                                            items = mediaDetailHolder.recommendations.items.map { it.toRailItem(strings, isManga) },
                                            layout = DetailListLayout.ROWS,
                                            onSelect = { state.openMedia(it) },
                                            bottomPadding = listBottom,
                                            hasMore = mediaDetailHolder.recommendations.hasNextPage,
                                            isLoadingMore = mediaDetailHolder.recommendations.isLoadingMore,
                                            onLoadMore = { mediaDetailHolder.loadMoreRecommendations() },
                                            actionError = mediaError,
                                            dismissLabel = strings.dismissAction,
                                            onDismissActionError = { mediaDetailHolder.clearActionError() },
                                        )
                                        DetailListKind.CHARACTER_MEDIA -> DetailListScreen(
                                            items = characterHolder.media.items.map { it.toRailItem(strings) },
                                            layout = DetailListLayout.ROWS,
                                            onSelect = { state.openMedia(it) },
                                            bottomPadding = listBottom,
                                            hasMore = characterHolder.media.hasNextPage,
                                            isLoadingMore = characterHolder.media.isLoadingMore,
                                            onLoadMore = { characterHolder.loadMore() },
                                            actionError = characterError,
                                            dismissLabel = strings.dismissAction,
                                            onDismissActionError = { characterHolder.clearActionError() },
                                        )
                                        DetailListKind.STAFF_MEDIA -> DetailListScreen(
                                            items = staffHolder.media.items.map { it.toRailItem() },
                                            layout = DetailListLayout.ROWS,
                                            onSelect = { state.openMedia(it) },
                                            bottomPadding = listBottom,
                                            hasMore = staffHolder.media.hasNextPage,
                                            isLoadingMore = staffHolder.media.isLoadingMore,
                                            onLoadMore = { staffHolder.loadMoreMedia() },
                                            actionError = staffError,
                                            dismissLabel = strings.dismissAction,
                                            onDismissActionError = { staffHolder.clearActionError() },
                                        )
                                        DetailListKind.STAFF_CHARACTERS -> DetailListScreen(
                                            items = staffHolder.characters.items.map { it.toRailItem() },
                                            layout = DetailListLayout.GRID,
                                            onSelect = { state.openCharacter(it) },
                                            bottomPadding = listBottom,
                                            hasMore = staffHolder.characters.hasNextPage,
                                            isLoadingMore = staffHolder.characters.isLoadingMore,
                                            onLoadMore = { staffHolder.loadMoreCharacters() },
                                            actionError = staffError,
                                            dismissLabel = strings.dismissAction,
                                            onDismissActionError = { staffHolder.clearActionError() },
                                        )
                                    }
                                }
                                null -> {}
                            }
                        } else if (state.isCalendarOpen) {
                            // Airing schedule overlay (fullscreen, tab bar hidden):
                            // public feed, so guests render the same surface.
                            CalendarScreen(
                                holder = calendarHolder,
                                onRetry = { calendarHolder.reloadSelected() },
                                bottomPadding = padding.calculateBottomPadding(),
                                isRefreshing = calendarHolder.isLoading,
                                onRefresh = { calendarHolder.reloadSelected() },
                                isFilterOpen = state.isCalendarFilterOpen,
                                onDismissFilter = { state.closeCalendarFilter() },
                                onOpenDetail = { state.openMedia(it) },
                                onOpenUrl = { urlOpener.open(it) },
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
                                isSmileyFormat = isSmileyFormat,
                                formatSmiley = formatSmiley,
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
                                isSmileyFormat = isSmileyFormat,
                                formatSmiley = formatSmiley,
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
                        } else if (activeTab == TabSection.DISCOVER) {
                            // Discover dashboard: public catalog, guests included.
                            // The branch above still owns the active-query state
                            // (Explore search lands later).
                            DashboardScreen(
                                holder = dashboardHolder,
                                onOpenMedia = { state.openMedia(it) },
                                hideAdult = !settingsState.displayAdultContent,
                                bottomPadding = padding.calculateBottomPadding(),
                            )
                        } else if (
                            (activeTab == TabSection.ANIME || activeTab == TabSection.MANGA ||
                                activeTab == TabSection.PROFILE) && viewerId == null
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
            val editorId = state.editorMediaId
            if (state.isEditorOpen && editorId != null && viewerId != null) {
                val editor = remember(editorId) {
                    createMediaEntryEditorState(
                        mediaId = editorId,
                        entryRepository = mediaListEntryRepository,
                        detailRepository = mediaDetailRepository,
                        scope = listScope,
                        stringsProvider = { currentStrings },
                        initialStatusOverride = state.editorInitialStatus,
                        initialProgressOverride = state.editorInitialProgress,
                        advancedScoringEnabled = settingsState.advancedScoring,
                        advancedScoringAnimeNames = settingsState.advancedScoringAnime,
                        advancedScoringMangaNames = settingsState.advancedScoringManga,
                        scoreMax = scoreMax,
                        onFavouriteChanged = { id, value -> mediaDetailHolder.applyFavouriteChange(id, value) },
                    )
                }
                LaunchedEffect(editorId) { editor.load() }
                MediaEntryEditorSheet(
                    editor = editor,
                    formatScore = formatScore,
                    parseScore = parseScore,
                    scoreSuffix = scoreSuffix,
                    scoreStep = scoreStep,
                    onSaved = {
                        state.closeEditor()
                        refreshLists()
                        mediaDetailHolder.onEntryChanged(editorId)
                    },
                    onDeleted = {
                        state.closeEditor()
                        refreshLists()
                        mediaDetailHolder.onEntryChanged(editorId)
                    },
                    onDismiss = { state.closeEditor() },
                )
            }
            val gridSortHolder = if (state.isDetailOpen && state.isDetailSortOpen) {
                gridSortHolderFor(state.currentDetail)
            } else {
                null
            }
            if (gridSortHolder != null) {
                // Shared sort sheet over the destination's grid holder (studio
                // or genre): picking a row re-queries the grid from page 1
                // (server order).
                SortSheet(
                    options = DetailGridSortOptions,
                    selected = gridSortHolder.sortOption,
                    onSelect = { gridSortHolder.changeSort(it, gridSortHolder.sortOrder) },
                    order = gridSortHolder.sortOrder,
                    onSelectOrder = { gridSortHolder.changeSort(gridSortHolder.sortOption, it) },
                    onDismiss = { state.closeDetailSort() },
                )
            }
            val detailMenu = detailOverflow
            if (state.isDetailOpen && state.isDetailMenuOpen && detailMenu != null) {
                ModalSheet(
                    title = detailMenu.title,
                    dismissLabel = strings.dismissAction,
                    onDismiss = { state.closeDetailMenu() },
                ) {
                    OptionGroup(title = null) {
                        OptionRow(
                            label = strings.mediaDetailOpenInAniListAction,
                            selected = false,
                            onClick = {
                                state.closeDetailMenu()
                                urlOpener.open(detailMenu.url)
                            },
                            divider = false,
                            trailing = {},
                        )
                        OptionRow(
                            label = strings.mediaDetailShareAction,
                            selected = false,
                            onClick = {
                                state.closeDetailMenu()
                                shareLauncher.share(text = detailMenu.shareText, subject = detailMenu.title)
                            },
                            trailing = {},
                        )
                    }
                }
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
        selectedIcon = AppIcons.DiscoverSelected,
    ),
    TabItem(
        section = TabSection.ANIME,
        label = TabSection.ANIME.label(strings),
        icon = AppIcons.Anime,
        selectedIcon = AppIcons.AnimeSelected,
    ),
    TabItem(
        section = TabSection.MANGA,
        label = TabSection.MANGA.label(strings),
        icon = AppIcons.Manga,
        selectedIcon = AppIcons.MangaSelected,
    ),
    TabItem(
        section = TabSection.PROFILE,
        label = TabSection.PROFILE.label(strings),
        icon = AppIcons.Profile,
        selectedIcon = AppIcons.ProfileSelected,
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

/** Per-destination overflow payload: what the detail menu opens/shares. */
private data class DetailOverflow(
    val title: String,
    val url: String,
    val shareText: String,
)

private const val ACTION_SEARCH = "search"
private const val ACTION_SETTINGS = "settings"
private const val ACTION_FILTER = "filter"
private const val ACTION_SORT = "sort"
private const val ACTION_CALENDAR = "calendar"
private const val ACTION_CALENDAR_FILTER = "calendar-filter"
private const val ACTION_EDIT = "edit"
private const val ACTION_FAVOURITE = "favourite"
private const val ACTION_MORE = "more"

/** Shared capsule group for the list tools (reference: one capsule, lone search). */
private const val GROUP_LIST_TOOLS = "list-tools"

@Composable
private fun toolbarActions(
    tab: TabSection,
    isSearching: Boolean,
    isSettingsOpen: Boolean,
    isCalendarOpen: Boolean,
    isDetailOpen: Boolean,
    isMediaDetail: Boolean,
    isStudioDetail: Boolean,
    isGenreDetail: Boolean,
    isDetailList: Boolean,
    isDetailFavourite: Boolean,
    isGuest: Boolean,
    strings: LanguageStrings,
): List<ToolbarAction> {
    if (isSearching || isSettingsOpen) return emptyList()
    // Detail overlay: edit / favourite / overflow trailing the back chevron.
    // The heart swaps to its filled twin when the entry is favourited.
    // Character/staff/studio details have no list entry, so only the heart.
    if (isDetailOpen) {
        val sort = ToolbarAction(
            id = ACTION_SORT,
            icon = AppIcons.Sort,
            contentDescription = strings.sortAction,
        )
        // A "View all" list has nothing but its back chevron.
        if (isDetailList) return emptyList()
        val favourite = ToolbarAction(
            id = ACTION_FAVOURITE,
            icon = if (isDetailFavourite) AppIcons.FavoriteFilled else AppIcons.Favorite,
            contentDescription = strings.favouriteLabel,
        )
        val more = ToolbarAction(
            id = ACTION_MORE,
            icon = AppIcons.More,
            contentDescription = strings.moreAction,
        )
        // Media adds the editor pencil, studio and genre their media sort; the
        // other scopes have no list entry. All open the overflow (AniList link +
        // share). A genre has no entity to favourite, so it skips the heart.
        if (isGenreDetail) {
            return listOf(
                sort,
                more,
            )
        }
        if (isStudioDetail) {
            return listOf(
                sort,
                favourite,
                more,
            )
        }
        if (!isMediaDetail) return listOf(favourite, more)
        return listOf(
            ToolbarAction(
                id = ACTION_EDIT,
                icon = AppIcons.Edit,
                contentDescription = strings.editEntryAction,
            ),
            favourite,
            more,
        )
    }
    // Calendar overlay: lone filter next to the back chevron. Settings
    // hides everything, but the day list needs its filters in reach.
    if (isCalendarOpen) {
        return listOf(
            ToolbarAction(
                id = ACTION_CALENDAR_FILTER,
                icon = AppIcons.Filter,
                contentDescription = strings.filterAction,
            ),
        )
    }
    // Gated tabs without a session keep the calendar only (public airing
    // data, no login needed): filter/sort/search operate on lists that
    // don't exist. The calendar button opens the schedule overlay, which
    // guests render with null user fields.
    val isGatedGuest = isGuest && (tab == TabSection.ANIME || tab == TabSection.MANGA)
    return buildList {
        if (tab != TabSection.PROFILE) {
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
        if (tab != TabSection.PROFILE) {
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
 * A child scope per state holder, derived from Root's `rememberCoroutineScope`
 * (same dispatcher, cancelled when Root leaves). It is recreated — and the old
 * one cancelled — when its [keys] change (a new viewer, a new holder), so an
 * in-flight request of a replaced holder can never write into its successor.
 */
@Composable
private fun rememberHolderScope(parent: CoroutineScope, vararg keys: Any?): CoroutineScope {
    val scope = remember(*keys) {
        CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
    }
    DisposableEffect(scope) {
        onDispose { scope.cancel() }
    }
    return scope
}

/**
 * Explicit opt-in per surface: the tab bar shows unless a surface asks to
 * hide it (full-screen surfaces like Settings). Default is visible.
 */
private fun showsTabBar(tab: TabSection, state: State): Boolean =
    !state.isSettingsOpen && !state.isCalendarOpen && !state.isDetailOpen

private fun toolbarTitle(
    state: State,
    tab: TabSection,
    strings: LanguageStrings,
): String = if (state.isSettingsOpen) {
    strings.settingsTitle
} else if (state.isCalendarOpen) {
    strings.calendarTitle
} else {
    tab.label(strings)
}

/** Toolbar title for a destination that carries its name in the bar instead of
 *  the content: a "View all" list and a genre page (both are lists). Null for
 *  every other destination — media/character/staff/studio keep their title in
 *  the hero, and tabs use [toolbarTitle]. */
private fun detailToolbarTitle(destination: DetailDestination?, strings: LanguageStrings): String? =
    when (destination) {
        is DetailDestination.DetailList -> when (destination.kind) {
            DetailListKind.MEDIA_CHARACTERS -> strings.mediaDetailCharactersTitle
            DetailListKind.MEDIA_STAFF -> strings.mediaDetailTabStaff
            DetailListKind.MEDIA_RELATED -> strings.mediaDetailRelationsTitle
            DetailListKind.MEDIA_RECOMMENDATIONS -> strings.mediaDetailTabRecommendations
            DetailListKind.CHARACTER_MEDIA -> strings.mediaDetailAppearancesTitle
            DetailListKind.STAFF_MEDIA -> strings.mediaDetailRolesTitle
            DetailListKind.STAFF_CHARACTERS -> strings.mediaDetailCharactersTitle
        }
        is DetailDestination.Genre -> destination.name
        else -> null
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
