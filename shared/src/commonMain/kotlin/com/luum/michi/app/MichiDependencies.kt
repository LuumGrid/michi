package com.luum.michi.app

import com.luum.michi.app.account.repository.AccountRepositoryImpl
import com.luum.michi.app.account.domain.AccountRepository
import com.luum.michi.app.mediaList.repository.anime.AnimeListRepositoryImpl
import com.luum.michi.app.mediaList.domain.anime.AnimeListRepository
import com.luum.michi.app.calendar.repository.CalendarRepositoryImpl
import com.luum.michi.app.calendar.domain.CalendarRepository
import com.luum.michi.app.mediaDetail.repository.character.CharacterDetailRepositoryImpl
import com.luum.michi.app.mediaDetail.domain.character.CharacterDetailRepository
import com.luum.michi.app.core.domain.auth.AniListOAuthLauncher
import com.luum.michi.app.core.domain.auth.AniListTokenStorage
import com.luum.michi.app.core.domain.auth.parseAniListOAuthCallback
import com.luum.michi.app.core.domain.medialist.MediaListEntryRepository
import com.luum.michi.app.core.domain.network.AniListGraphQLClient
import com.luum.michi.app.core.repository.network.KtorAniListGraphQLClient
import com.luum.michi.app.core.repository.network.createAniListHttpClient
import com.luum.michi.app.core.domain.session.AniListViewerRepository
import com.luum.michi.app.core.repository.session.AniListViewerRepositoryImpl
import com.luum.michi.app.core.domain.session.SessionManager
import com.luum.michi.app.discover.repository.DashboardRepositoryImpl
import com.luum.michi.app.discover.repository.ExploreRepositoryImpl
import com.luum.michi.app.discover.domain.DashboardRepository
import com.luum.michi.app.discover.domain.ExploreRepository
import com.luum.michi.app.mediaList.repository.manga.MangaListRepositoryImpl
import com.luum.michi.app.mediaList.domain.manga.MangaListRepository
import com.luum.michi.app.mediaDetail.repository.media.MediaDetailRepositoryImpl
import com.luum.michi.app.mediaDetail.repository.media.MediaListEntryRepositoryImpl
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
import kotlinx.coroutines.launch

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
) {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    internal val httpClient: HttpClient = createAniListHttpClient()
    internal val graphQLClient: AniListGraphQLClient = KtorAniListGraphQLClient(
        httpClient = httpClient,
        tokenProvider = { tokenStorage.load() },
    )
    internal val viewerRepository: AniListViewerRepository =
        AniListViewerRepositoryImpl(graphQLClient)

    internal val animeListRepository: AnimeListRepository =
        AnimeListRepositoryImpl(graphQLClient)

    internal val mangaListRepository: MangaListRepository =
        MangaListRepositoryImpl(graphQLClient)

    internal val accountRepository: AccountRepository =
        AccountRepositoryImpl(graphQLClient)

    internal val dashboardRepository: DashboardRepository =
        DashboardRepositoryImpl(graphQLClient)

    internal val exploreRepository: ExploreRepository =
        ExploreRepositoryImpl(graphQLClient)

    internal val calendarRepository: CalendarRepository =
        CalendarRepositoryImpl(graphQLClient)

    internal val mediaDetailRepository: MediaDetailRepository =
        MediaDetailRepositoryImpl(graphQLClient)

    internal val mediaListEntryRepository: MediaListEntryRepository =
        MediaListEntryRepositoryImpl(graphQLClient)

    internal val notificationsRepository: NotificationsRepository =
        NotificationsRepositoryImpl(graphQLClient)

    internal val studioDetailRepository: StudioDetailRepository =
        StudioDetailRepositoryImpl(graphQLClient)

    internal val characterDetailRepository: CharacterDetailRepository =
        CharacterDetailRepositoryImpl(graphQLClient)

    internal val staffDetailRepository: StaffDetailRepository =
        StaffDetailRepositoryImpl(graphQLClient)

    internal val settingsRepository: SettingsRepository =
        SettingsRepositoryImpl(graphQLClient)

    internal val sessionManager: SessionManager =
        SessionManager(tokenStorage, viewerRepository)

    /**
     * Bootstraps the app: checks persisted token, fetches viewer if present.
     * Called once at app start by the platform entry.
     */
    fun bootstrap() {
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

    fun logout() {
        scope.launch { sessionManager.logout() }
    }
}
