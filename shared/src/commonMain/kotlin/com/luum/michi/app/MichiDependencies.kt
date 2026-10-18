package com.luum.michi.app

import com.luum.michi.app.core.favourite.domain.FavouriteRepository
import com.luum.michi.app.core.favourite.repository.FavouriteRepositoryImpl
import com.luum.michi.app.profile.repository.ProfileRepositoryImpl
import com.luum.michi.app.profile.domain.ProfileRepository
import com.luum.michi.app.mediaList.repository.anime.AnimeListRepositoryImpl
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.calendar.repository.CalendarRepositoryImpl
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.mediaDetail.repository.character.CharacterDetailRepositoryImpl
import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.core.auth.domain.AniListOAuthLauncher
import com.luum.michi.app.core.platform.domain.UrlOpener
import com.luum.michi.app.core.platform.domain.ShareLauncher
import com.luum.michi.app.core.navigation.domain.parseAniListMediaId
import com.luum.michi.app.core.auth.domain.AniListTokenStorage
import com.luum.michi.app.core.auth.domain.parseAniListOAuthCallback
import com.luum.michi.app.core.medialist.domain.MediaListEntryRepository
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.repository.KtorAniListGraphQLClient
import com.luum.michi.app.core.network.repository.createAniListHttpClient
import com.luum.michi.app.core.session.domain.AniListViewerRepository
import com.luum.michi.app.core.session.repository.AniListViewerRepositoryImpl
import com.luum.michi.app.core.session.domain.SessionManager
import com.luum.michi.app.core.session.repository.SettingsStoreCachedViewerStore
import com.luum.michi.app.core.storage.domain.SettingsStore
import com.luum.michi.app.core.platform.domain.AlwaysUnmeteredConnectivity
import com.luum.michi.app.core.platform.domain.Connectivity
import com.luum.michi.app.discover.repository.DashboardRepositoryImpl
import com.luum.michi.app.discover.repository.ExploreRepositoryImpl
import com.luum.michi.app.discover.domain.DashboardRepository
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.mediaList.repository.manga.MangaListRepositoryImpl
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.mediaDetail.repository.media.MediaDetailRepositoryImpl
import com.luum.michi.app.core.medialist.repository.MediaListEntryRepositoryImpl
import com.luum.michi.app.mediaDetail.domain.media.MediaDetailRepository
import com.luum.michi.app.notifications.repository.NotificationsRepositoryImpl
import com.luum.michi.app.notifications.domain.NotificationsRepository
import com.luum.michi.app.settings.repository.SettingsRepositoryImpl
import com.luum.michi.app.settings.domain.SettingsRepository
import com.luum.michi.app.mediaDetail.repository.staff.StaffDetailRepositoryImpl
import com.luum.michi.app.mediaDetail.domain.staff.StaffDetailRepository
import com.luum.michi.app.mediaDetail.repository.studio.StudioDetailRepositoryImpl
import com.luum.michi.app.mediaDetail.domain.studio.StudioDetailRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.luum.michi.app.discover.repository.clearLegacyDashboardCache
import com.luum.michi.app.mediaDetail.domain.common.ExternalLinkCatalogRepository
import com.luum.michi.app.mediaDetail.repository.ExternalLinkCatalogRepositoryImpl

/**
 * App-level composition root. Lives exactly once per process: owned by
 * `MichiApplication` on Android and by the `IosMichiDependencies` holder on
 * iOS, which provide the platform-specific [tokenStorage] and [oAuthLauncher].
 * Never construct it from a recreated UI entry (Activity / view controller),
 * or each recreation would leak a CoroutineScope + HttpClient.
 *
 * The Android entry must also dispatch deep links (`michi://oauth/callback`) to
 * [onOAuthCallback]; iOS does the same from its `onOpenURL` handler.
 */
class MichiDependencies internal constructor(
    internal val tokenStorage: AniListTokenStorage,
    internal val oAuthLauncher: AniListOAuthLauncher,
    internal val urlOpener: UrlOpener,
    internal val shareLauncher: ShareLauncher,
    internal val settingsStore: SettingsStore,
    /** Network metering for AUTO image quality; platform impls override it. */
    internal val connectivity: Connectivity = AlwaysUnmeteredConnectivity(),
    /** Debug builds log GraphQL timing; platform entries pass the real flag. */
    internal val isDebug: Boolean = false,
) {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    internal val httpClient: HttpClient = createAniListHttpClient()
    internal val graphQLClient: AniListGraphQLClient = KtorAniListGraphQLClient(
        httpClient = httpClient,
        // Never send an expired token: it would 401 forever, so an expired stored
        // token closes the session instead.
        tokenProvider = {
            tokenStorage.load()?.let { token ->
                if (token.isExpired) {
                    sessionManager.onUnauthorized(token)
                    null
                } else {
                    token
                }
            }
        },
        debugLogging = isDebug,
        // A mid-session 401 closes the session only if that token is still the
        // stored one (a stale 401 never closes a newer session).
        onUnauthorized = { token -> scope.launch { sessionManager.onUnauthorized(token) } },
    )
    internal val viewerRepository: AniListViewerRepository =
        AniListViewerRepositoryImpl(graphQLClient)

    internal val animeListRepository: AnimeListRepository =
        AnimeListRepositoryImpl(graphQLClient)

    internal val mangaListRepository: MangaListRepository =
        MangaListRepositoryImpl(graphQLClient)

    internal val profileRepository: ProfileRepository =
        ProfileRepositoryImpl(graphQLClient)

    internal val externalLinkCatalogRepository: ExternalLinkCatalogRepository =
        ExternalLinkCatalogRepositoryImpl(graphQLClient, settingsStore, scope)

    internal val dashboardRepository: DashboardRepository =
        DashboardRepositoryImpl(graphQLClient)

    internal val exploreRepository: ExploreRepository =
        ExploreRepositoryImpl(graphQLClient)

    internal val calendarRepository: CalendarRepository =
        CalendarRepositoryImpl(graphQLClient)

    internal val mediaDetailRepository: MediaDetailRepository =
        MediaDetailRepositoryImpl(graphQLClient)

    internal val favouriteRepository: FavouriteRepository =
        FavouriteRepositoryImpl(graphQLClient)

    internal val mediaListEntryRepository: MediaListEntryRepository =
        MediaListEntryRepositoryImpl(graphQLClient, favouriteRepository)

    internal val notificationsRepository: NotificationsRepository =
        NotificationsRepositoryImpl(graphQLClient)

    internal val studioDetailRepository: StudioDetailRepository =
        StudioDetailRepositoryImpl(graphQLClient, favouriteRepository)

    internal val characterDetailRepository: CharacterDetailRepository =
        CharacterDetailRepositoryImpl(graphQLClient, externalLinkCatalogRepository, favouriteRepository)

    internal val staffDetailRepository: StaffDetailRepository =
        StaffDetailRepositoryImpl(graphQLClient, externalLinkCatalogRepository, favouriteRepository)

    internal val settingsRepository: SettingsRepository =
        SettingsRepositoryImpl(graphQLClient)

    internal val sessionManager: SessionManager =
        SessionManager(tokenStorage, viewerRepository, SettingsStoreCachedViewerStore(settingsStore))

    /**
     * Bootstraps the app: clears the removed dashboard disk cache, then checks
     * the persisted token and fetches the viewer if present.
     * Called once at app start by the platform entry.
     */
    fun bootstrap() {
        clearLegacyDashboardCache(settingsStore)
        scope.launch { sessionManager.bootstrap() }
    }

    /**
     * Called by the platform when the OAuth deep link fires. Parses the URI
     * fragment and, on success, stores the token and refreshes the session.
     * No-op when the URI is not a valid AniList callback.
     */
    fun onOAuthCallback(uri: String) {
        val token = parseAniListOAuthCallback(uri) ?: return
        scope.launch { sessionManager.onOAuthCallback(token) }
    }

    /**
     * Deep-linked media id from an AniList web link, or null. The UI collects
     * this and opens the detail page, then calls [consumeDeepLink].
     */
    private val _pendingMediaId = MutableStateFlow<Int?>(null)
    internal val pendingMediaId: StateFlow<Int?> = _pendingMediaId.asStateFlow()

    /** Called by the platform when an AniList media link is opened. No-op otherwise. */
    fun onDeepLink(url: String) {
        parseAniListMediaId(url)?.let { _pendingMediaId.value = it }
    }

    fun consumeDeepLink() {
        _pendingMediaId.value = null
    }

    fun logout() {
        scope.launch { sessionManager.logout() }
    }
}
