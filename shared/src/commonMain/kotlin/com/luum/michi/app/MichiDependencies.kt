package com.luum.michi.app

import com.luum.michi.app.core.auth.AniListOAuthLauncher
import com.luum.michi.app.core.auth.AniListTokenStorage
import com.luum.michi.app.core.auth.parseAniListOAuthCallback
import com.luum.michi.app.core.network.AniListGraphQLClient
import com.luum.michi.app.core.network.KtorAniListGraphQLClient
import com.luum.michi.app.core.network.createAniListHttpClient
import com.luum.michi.app.core.session.AniListViewerRepository
import com.luum.michi.app.core.session.AniListViewerRepositoryImpl
import com.luum.michi.app.core.session.SessionManager
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
