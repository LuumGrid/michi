package com.luum.michi.app

import com.luum.michi.app.account.data.AccountRepository
import com.luum.michi.app.account.data.AccountRepositoryImpl
import com.luum.michi.app.animation.data.AnimationListRepository
import com.luum.michi.app.animation.data.AnimationListRepositoryImpl
import com.luum.michi.app.explore.data.ExploreRepository
import com.luum.michi.app.explore.data.ExploreRepositoryImpl
import com.luum.michi.app.calendar.data.CalendarRepository
import com.luum.michi.app.calendar.data.CalendarRepositoryImpl
import com.luum.michi.app.core.auth.AniListOAuthLauncher
import com.luum.michi.app.core.auth.AniListTokenStorage
import com.luum.michi.app.core.auth.parseAniListOAuthCallback
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.KtorAniListGraphQLClient
import com.luum.michi.app.core.network.createAniListHttpClient
import com.luum.michi.app.core.session.AniListViewerRepository
import com.luum.michi.app.core.session.AniListViewerRepositoryImpl
import com.luum.michi.app.core.session.SessionManager
import com.luum.michi.app.dashboard.data.DashboardRepository
import com.luum.michi.app.dashboard.data.DashboardRepositoryImpl
import com.luum.michi.app.mediaDetail.data.MediaDetailRepository
import com.luum.michi.app.mediaDetail.data.MediaDetailRepositoryImpl
import com.luum.michi.app.mediaDetail.data.MediaListEntryRepository
import com.luum.michi.app.mediaDetail.data.MediaListEntryRepositoryImpl
import com.luum.michi.app.feed.data.FeedRepository
import com.luum.michi.app.feed.data.FeedRepositoryImpl
import com.luum.michi.app.reading.data.ReadingListRepository
import com.luum.michi.app.reading.data.ReadingListRepositoryImpl
import com.luum.michi.app.search.data.SearchRepository
import com.luum.michi.app.search.data.SearchRepositoryImpl
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * App-level composition root. Constructed by each platform's entry point
 * (`MainActivity` on Android, `MainViewController` on iOS) which provides the
 * platform-specific [tokenStorage] and [oAuthLauncher].
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

    internal val animationListRepository: AnimationListRepository =
        AnimationListRepositoryImpl(graphQLClient)

    internal val readingListRepository: ReadingListRepository =
        ReadingListRepositoryImpl(graphQLClient)

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

    internal val searchRepository: SearchRepository =
        SearchRepositoryImpl(graphQLClient)

    internal val feedRepository: FeedRepository =
        FeedRepositoryImpl(graphQLClient)

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
