package com.luum.michi.app.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.calendar.ui.state.CalendarStateHolder
import com.luum.michi.app.calendar.ui.state.createCalendarStateHolder
import com.luum.michi.app.core.favourite.domain.FavouriteRepository
import com.luum.michi.app.core.favourite.domain.FavouriteTarget
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.medialist.domain.SortPersistence
import com.luum.michi.app.core.navigation.domain.DetailDestination
import com.luum.michi.app.core.navigation.domain.DetailHolder
import com.luum.michi.app.core.navigation.domain.GridSortHolder
import com.luum.michi.app.discover.domain.DashboardRepository
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.discover.ui.catalog.state.CatalogGridStateHolder
import com.luum.michi.app.discover.ui.dashboard.state.DashboardStateHolder
import com.luum.michi.app.discover.ui.dashboard.state.createDashboardStateHolder
import com.luum.michi.app.discover.ui.explore.state.ExploreStateHolder
import com.luum.michi.app.discover.ui.explore.state.createExploreStateHolder
import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import com.luum.michi.app.mediaDetail.ui.character.state.CharacterDetailStateHolder
import com.luum.michi.app.mediaDetail.ui.media.state.MediaDetailStateHolder
import com.luum.michi.app.mediaDetail.ui.staff.state.StaffDetailStateHolder
import com.luum.michi.app.mediaDetail.ui.studio.state.StudioDetailStateHolder
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.mediaList.ui.anime.state.AnimeListStateHolder
import com.luum.michi.app.mediaList.ui.manga.state.MangaListStateHolder
import com.luum.michi.app.notifications.domain.NotificationsRepository
import com.luum.michi.app.notifications.ui.state.NotificationsStateHolder
import com.luum.michi.app.notifications.ui.state.createNotificationsStateHolder
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.profile.ui.state.ProfileFavoritesGridStateHolder
import com.luum.michi.app.profile.ui.state.ProfileStateHolder
import com.luum.michi.app.profile.ui.state.createProfileFavoritesGridStateHolder
import com.luum.michi.app.profile.ui.state.createProfileStateHolder
import com.luum.michi.app.root.state.toDashboardMediaType
import com.luum.michi.app.settings.ui.state.SettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * The shell's state holders, created once per composition and shared by the
 * effects and the surfaces. One instance behind every detail contract, so
 * refresh, cache invalidation and the favourite action resolve the active
 * destination through a single lookup.
 */
@Stable
internal class RootHolders(
    val listScope: CoroutineScope,
    val calendar: CalendarStateHolder,
    val anime: AnimeListStateHolder?,
    val manga: MangaListStateHolder?,
    val dashboard: DashboardStateHolder,
    val explore: ExploreStateHolder,
    val mediaDetail: MediaDetailStateHolder,
    val character: CharacterDetailStateHolder,
    val staff: StaffDetailStateHolder,
    val studio: StudioDetailStateHolder,
    val catalog: CatalogGridStateHolder,
    val profile: ProfileStateHolder?,
    val profileFavorites: ProfileFavoritesGridStateHolder?,
    val notifications: NotificationsStateHolder?,
    /**
     * Single favourite-change router: applies a change to the detail holder of
     * its target kind (visible page + cached snapshot, no network). Used by the
     * profile's remove action and the entry editor, so neither feature imports
     * the other.
     */
    val onFavouriteChanged: (FavouriteTarget, Boolean) -> Unit,
) {
    // Every pushed detail page lives behind one contract, so refresh, cache
    // invalidation and the favourite action are a single lookup instead of a
    // five-branch `when` per call site.
    val detailHolders: List<DetailHolder> = listOf(
        mediaDetail,
        character,
        staff,
        studio,
        catalog,
    )
    fun detailHolderFor(destination: DetailDestination?): DetailHolder? = when (destination) {
        is DetailDestination.Media -> mediaDetail
        is DetailDestination.Character -> character
        is DetailDestination.Staff -> staff
        is DetailDestination.Studio -> studio
        is DetailDestination.Genre,
        is DetailDestination.Catalog -> catalog
        is DetailDestination.ProfileFavorites -> null
        is DetailDestination.DetailList -> null
        null -> null
    }
    // Only the server-ordered grids (studio, genre, catalog) open the detail sort sheet.
    fun gridSortHolderFor(destination: DetailDestination?): GridSortHolder? = when (destination) {
        is DetailDestination.Studio -> studio
        is DetailDestination.Genre,
        is DetailDestination.Catalog -> catalog
        else -> null
    }
}

/**
 * Creates the shell's state holders with their original keys and returns them as
 * one stable [RootHolders]. Holder loads stay explicit (nothing fires from a
 * `remember`); the scopes are cancelled when they are replaced.
 */
@Composable
internal fun rememberRootHolders(
    viewerId: Int?,
    settingsState: SettingsState,
    strings: LanguageStrings,
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
    profileRepository: ProfileRepository,
    favouriteRepository: FavouriteRepository,
    notificationsRepository: NotificationsRepository,
    sortPersistence: SortPersistence,
): RootHolders {
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

    // Discover dashboard holder: session-independent (public catalog) and
    // shared by guests. Loads are explicit from the screen; the TTL dedups
    // revisits and the 18+ gate lives in the holder. The holder is keyed
    // without `strings` so a language change keeps the Anime/Manga selection;
    // the provider is read at launch so one request never mixes languages.
    val dashboardScope = rememberHolderScope(listScope)
    val dashboardHolder = remember(dashboardRepository, dashboardScope) {
        createDashboardStateHolder(
            repository = dashboardRepository,
            scope = dashboardScope,
            hideAdult = !settingsState.displayAdultContent,
            // "Pestaña al abrir" → DISCOVER sub-choice; startup only, no load.
            initialType = settingsState.startCatalogType.toDashboardMediaType(),
        ) { currentStrings }
    }

    // Explore holder: the DISCOVER search surface. Session-independent catalog
    // but `onList` depends on the session, so it is re-keyed per viewer; the
    // provider is read at launch so one request never mixes languages.
    val exploreScope = rememberHolderScope(listScope, viewerId)
    val exploreHolder = remember(exploreRepository, exploreScope, viewerId) {
        createExploreStateHolder(
            repository = exploreRepository,
            scope = exploreScope,
            stringsProvider = { currentStrings },
            initialHideAdult = !settingsState.displayAdultContent,
        )
    }

    // Detail holders: session-independent data but per-user favourite state, so
    // they are re-keyed per viewer and read strings through a provider (a
    // language change keeps the holder and its cache). Each owns a child scope
    // cancelled when it is replaced.
    val mediaDetailScope = rememberHolderScope(listScope, viewerId)
    val mediaDetailHolder = remember(mediaDetailRepository, mediaListEntryRepository, mediaDetailScope, viewerId) {
        MediaDetailStateHolder(
            mediaDetailRepository,
            mediaListEntryRepository,
            mediaDetailScope,
        ) { currentStrings }
    }
    val characterScope = rememberHolderScope(listScope, viewerId)
    val characterHolder = remember(characterDetailRepository, characterScope, viewerId) {
        CharacterDetailStateHolder(characterDetailRepository, characterScope) { currentStrings }
    }
    val staffScope = rememberHolderScope(listScope, viewerId)
    val staffHolder = remember(staffDetailRepository, staffScope, viewerId) {
        StaffDetailStateHolder(staffDetailRepository, staffScope) { currentStrings }
    }
    val studioScope = rememberHolderScope(listScope, viewerId)
    val studioHolder = remember(studioDetailRepository, studioScope, viewerId) {
        StudioDetailStateHolder(studioDetailRepository, studioScope)
    }
    val catalogScope = rememberHolderScope(listScope, viewerId)
    val catalogHolder = remember(exploreRepository, catalogScope, viewerId) {
        CatalogGridStateHolder(
            repository = exploreRepository,
            scope = catalogScope,
            hideAdult = !settingsState.displayAdultContent,
        )
    }

    // Profile: the viewer's own profile (guests get null — the tab shows the
    // sign-in prompt). Recreated per viewer, like the lists, so one viewer's
    // cached state can never bleed into another's.
    // Stable across recompositions: the router is a key of the final remember
    // (and of profileHolder), so a fresh lambda would recreate RootHolders on
    // every Root recomposition.
    val favouriteRouter: (FavouriteTarget, Boolean) -> Unit = remember(
        mediaDetailHolder,
        characterHolder,
        staffHolder,
        studioHolder,
    ) {
        { target, value ->
            when (target) {
                is FavouriteTarget.Anime,
                is FavouriteTarget.Manga -> mediaDetailHolder.applyFavourite(target.id, value)
                is FavouriteTarget.Character -> characterHolder.applyFavourite(target.id, value)
                is FavouriteTarget.Staff -> staffHolder.applyFavourite(target.id, value)
                is FavouriteTarget.Studio -> studioHolder.applyFavourite(target.id, value)
            }
        }
    }
    // Notifications: the feed holder, session-gated (the overlay entry is the
    // PROFILE bell). It owns the unread dot; the profile only feeds it.
    val notificationsScope = rememberHolderScope(listScope, viewerId)
    val notificationsHolder = remember(viewerId, notificationsRepository, notificationsScope) {
        viewerId?.let { createNotificationsStateHolder(notificationsRepository, notificationsScope) }
    }
    val profileScope = rememberHolderScope(listScope, viewerId)
    val profileHolder = remember(
        viewerId,
        profileRepository,
        favouriteRepository,
        profileScope,
    ) {
        viewerId?.let {
            createProfileStateHolder(
                repository = profileRepository,
                favouriteRepository = favouriteRepository,
                scope = profileScope,
                onUnreadNotificationCount = { count -> notificationsHolder?.updateUnreadCount(count) },
            )
        }
    }
    val profileFavoritesScope = rememberHolderScope(listScope, viewerId)
    val profileFavoritesHolder = remember(
        viewerId,
        profileRepository,
        favouriteRepository,
        profileFavoritesScope,
        favouriteRouter,
    ) {
        viewerId?.let {
            createProfileFavoritesGridStateHolder(
                repository = profileRepository,
                favouriteRepository = favouriteRepository,
                scope = profileFavoritesScope,
                onFavouriteChanged = favouriteRouter,
                onFavoriteRemoved = { category, id -> profileHolder?.removeFavoriteLocally(category, id) },
            )
        }
    }

    return remember(
        listScope,
        calendarHolder,
        animeHolder,
        mangaHolder,
        dashboardHolder,
        exploreHolder,
        mediaDetailHolder,
        characterHolder,
        staffHolder,
        studioHolder,
        catalogHolder,
        profileHolder,
        profileFavoritesHolder,
        notificationsHolder,
        favouriteRouter,
    ) {
        RootHolders(
            listScope = listScope,
            calendar = calendarHolder,
            anime = animeHolder,
            manga = mangaHolder,
            dashboard = dashboardHolder,
            explore = exploreHolder,
            mediaDetail = mediaDetailHolder,
            character = characterHolder,
            staff = staffHolder,
            studio = studioHolder,
            catalog = catalogHolder,
            profile = profileHolder,
            profileFavorites = profileFavoritesHolder,
            notifications = notificationsHolder,
            onFavouriteChanged = favouriteRouter,
        )
    }
}

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
