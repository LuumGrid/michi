package com.luum.michi.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luum.michi.app.auth.presentation.AuthLandingScreen
import com.luum.michi.app.auth.presentation.AuthLoadingScreen
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.language.ProvideLanguageStrings
import com.luum.michi.app.core.language.currentPlatformLanguageCode
import com.luum.michi.app.core.session.SessionState
import com.luum.michi.app.shell.ShellScreen

@Composable
fun App(
    dependencies: MichiDependencies,
    initialLanguage: AppLanguage = AppLanguage.fromCode(currentPlatformLanguageCode()),
) {
    val systemDark = isSystemInDarkTheme()
    var isDarkMode by remember { mutableStateOf(systemDark) }
    var language by remember(initialLanguage) { mutableStateOf(initialLanguage) }

    LaunchedEffect(dependencies) {
        dependencies.bootstrap()
    }

    val sessionState by dependencies.sessionManager.state.collectAsStateWithLifecycle()

    MaterialTheme(
        colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme(),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            ProvideLanguageStrings(language) {
                when (val state = sessionState) {
                    is SessionState.Loading -> AuthLoadingScreen()
                    is SessionState.Anonymous -> AuthLandingScreen(
                        onLoginClick = { dependencies.oAuthLauncher.open() },
                    )
                    is SessionState.Error -> AuthLandingScreen(
                        onLoginClick = { dependencies.oAuthLauncher.open() },
                        errorMessage = state.message,
                    )
                    is SessionState.Authenticated -> ShellScreen(
                        viewer = state.viewer,
                        animationListRepository = dependencies.animationListRepository,
                        readingListRepository = dependencies.readingListRepository,
                        accountStatsRepository = dependencies.accountStatsRepository,
                        accountFavoritesRepository = dependencies.accountFavoritesRepository,
                        dashboardRepository = dependencies.dashboardRepository,
                        exploreRepository = dependencies.exploreRepository,
                        calendarRepository = dependencies.calendarRepository,
                        mediaDetailRepository = dependencies.mediaDetailRepository,
                        mediaListEntryRepository = dependencies.mediaListEntryRepository,
                        searchRepository = dependencies.searchRepository,
                        language = language,
                        onLanguageChange = { language = it },
                        isDarkMode = isDarkMode,
                        onToggleTheme = { isDarkMode = !isDarkMode },
                        onLogout = { dependencies.logout() },
                    )
                }
            }
        }
    }
}
