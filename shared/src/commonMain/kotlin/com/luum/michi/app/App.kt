package com.luum.michi.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.luum.michi.app.ui.language.LanguageProvider
import com.luum.michi.app.ui.language.ProvideLanguageStrings
import com.luum.michi.app.core.language.currentLanguageCode
import com.luum.michi.app.core.language.networkErrorMessage
import com.luum.michi.app.ui.SettingsStoreKeys
import com.luum.michi.app.ui.MichiDarkAmoledColorScheme
import com.luum.michi.app.ui.MichiLightColorScheme
import com.luum.michi.app.ui.rememberSettingsStore
import com.luum.michi.app.core.session.SessionState
import com.luum.michi.app.root.Root

@Composable
fun App(
    dependencies: MichiDependencies,
    initialLanguage: AppLanguage = AppLanguage.fromCode(currentLanguageCode()),
) {
    val store = rememberSettingsStore()
    val systemDark = isSystemInDarkTheme()
    // Theme + language are owned here (above the authenticated shell), so they must read their
    // persisted value at startup — SettingsState writes both keys but can't restore them this early.
    val initialDark = when (store.getString(SettingsStoreKeys.ThemeMode)) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemDark // SYSTEM or unset
    }
    var isDarkMode by remember { mutableStateOf(initialDark) }
    var language by remember {
        mutableStateOf(
            store.getString(SettingsStoreKeys.Language)?.let { AppLanguage.fromCode(it) } ?: initialLanguage,
        )
    }

    LaunchedEffect(dependencies) {
        dependencies.bootstrap()
    }

    val sessionState by dependencies.sessionManager.state.collectAsStateWithLifecycle()

    MaterialTheme(
        colorScheme = if (isDarkMode) MichiDarkAmoledColorScheme else MichiLightColorScheme,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            ProvideLanguageStrings(language) {
                val strings = LanguageProvider.strings
                when (val state = sessionState) {
                    is SessionState.Loading -> AuthLoadingScreen()
                    is SessionState.Anonymous -> AuthLandingScreen(
                        onLoginClick = { dependencies.oAuthLauncher.open() },
                    )
                    is SessionState.Error -> AuthLandingScreen(
                        onLoginClick = { dependencies.oAuthLauncher.open() },
                        errorMessage = strings.networkErrorMessage(state.error),
                    )
                    is SessionState.Authenticated -> Root(
                        viewer = state.viewer,
                        animeListRepository = dependencies.animeListRepository,
                        mangaListRepository = dependencies.mangaListRepository,
                        accountRepository = dependencies.accountRepository,
                        dashboardRepository = dependencies.dashboardRepository,
                        exploreRepository = dependencies.exploreRepository,
                        calendarRepository = dependencies.calendarRepository,
                        mediaDetailRepository = dependencies.mediaDetailRepository,
                        mediaListEntryRepository = dependencies.mediaListEntryRepository,
                        notificationsRepository = dependencies.notificationsRepository,
                        studioDetailRepository = dependencies.studioDetailRepository,
                        characterDetailRepository = dependencies.characterDetailRepository,
                        staffDetailRepository = dependencies.staffDetailRepository,
                        settingsRepository = dependencies.settingsRepository,
                        language = language,
                        onLanguageChange = {
                            language = it
                            store.putString(SettingsStoreKeys.Language, it.code)
                        },
                        isDarkMode = isDarkMode,
                        onToggleTheme = { isDarkMode = !isDarkMode },
                        onLogout = { dependencies.logout() },
                    )
                }
            }
        }
    }
}
