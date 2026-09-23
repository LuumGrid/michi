package com.luum.michi.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.auth.domain.AniListOAuthConfig
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.language.domain.getLanguageStrings
import com.luum.michi.app.core.language.domain.networkErrorMessage
import com.luum.michi.app.core.session.domain.SessionState
import com.luum.michi.app.core.storage.domain.SettingsStoreKeys
import com.luum.michi.app.root.AuthLandingScreen
import com.luum.michi.app.root.AuthService
import com.luum.michi.app.root.Root
import com.luum.michi.app.settings.ui.state.rememberSettingsState
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.components.tabFadeSpec
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.theme.paletteForId
import com.luum.michi.app.ui.theme.paletteIdOf
import com.luum.michi.app.ui.language.LocalStrings
import com.luum.michi.app.ui.theme.AppFont
import com.luum.michi.app.ui.theme.Theme
import com.luum.michi.app.ui.theme.ThemeColors
import com.luum.michi.app.ui.theme.ThemeType
import kotlinx.coroutines.launch

/**
 * App entry: owns theme (palette + mode + font) + language state and routes
 * between the auth landing and the shell based on [SessionState].
 * Called once from each platform entry.
 */
@Composable
fun App(
    dependencies: MichiDependencies,
) {
    // Local prefs: hydrated from the store once, persisted on every change.
    // Unknown/corrupt values fall back to defaults, never crash.
    val store = dependencies.settingsStore
    var language by remember {
        mutableStateOf(AppLanguage.fromCode(store.getString(SettingsStoreKeys.Language)))
    }
    var guestMode by remember { mutableStateOf(false) }
    var palette: ThemeColors by remember {
        mutableStateOf(paletteForId(store.getString(SettingsStoreKeys.ThemePalette)))
    }
    var themeType by remember {
        mutableStateOf(
            ThemeType.entries.firstOrNull { it.name == store.getString(SettingsStoreKeys.ThemeMode) }
                ?: ThemeType.SYSTEM,
        )
    }
    var font by remember {
        mutableStateOf(
            AppFont.entries.firstOrNull { it.name == store.getString(SettingsStoreKeys.Font) }
                ?: AppFont.JAKARTA,
        )
    }
    val onLanguageChange: (AppLanguage) -> Unit = {
        language = it
        store.putString(SettingsStoreKeys.Language, it.code)
    }

    val strings = getLanguageStrings(language)
    val session by dependencies.sessionManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    // Synced settings holder: one per session. Guests get canSync = false
    // (local-only, never touches the network); login/logout recreates it.
    val settingsState = rememberSettingsState(
        repository = dependencies.settingsRepository,
        store = dependencies.settingsStore,
        scope = scope,
        canSync = session is SessionState.Authenticated,
    )

    CompositionLocalProvider(LocalStrings provides strings) {
        Theme(
            palette = palette,
            type = themeType,
            font = font,
        ) {
            val current = session
            val route = when (current) {
                SessionState.Loading -> AppRoute.LOADING
                SessionState.Anonymous -> if (guestMode) AppRoute.SHELL else AppRoute.LANDING
                is SessionState.Authenticated -> AppRoute.SHELL
                is SessionState.Error -> AppRoute.ERROR
            }
            // Same motion language as the tab switch: fade + quarter slide
            // with tabFadeSpec. Entering the shell is forward, everything
            // else is backward.
            // Opaque theme backdrop: this AnimatedContent sits outside every
            // Scaffold, so the crossfade alpha overlap would otherwise bleed
            // the (light) window background through. (Tab switches don't need
            // this: they compose inside Root's opaque Scaffold.)
            AnimatedContent(
                targetState = route,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                transitionSpec = {
                    val forward = initialState == AppRoute.LANDING && targetState == AppRoute.SHELL
                    val enterFrom = if (forward) 1 else -1
                    (fadeIn(animationSpec = tabFadeSpec()) +
                        slideInHorizontally(animationSpec = tabFadeSpec()) { it / 4 * enterFrom } togetherWith
                        fadeOut(animationSpec = tabFadeSpec()) +
                        slideOutHorizontally(animationSpec = tabFadeSpec()) { -it / 4 * enterFrom }) using
                        SizeTransform(clip = false)
                },
                label = "session-route",
            ) { activeRoute ->
                when (activeRoute) {
                    AppRoute.LOADING -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = strings.authLoadingLabel,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    AppRoute.LANDING -> {
                        AuthLandingScreen(
                            strings = strings,
                            services = listOf(
                                AuthService(
                                    id = "anilist",
                                    label = strings.authLoginAction,
                                    onLogin = { dependencies.oAuthLauncher.open() },
                                    icon = AppIcons.AniList,
                                    brandHex = ANILIST_BRAND_HEX,
                                ),
                            ),
                            language = language,
                            onLanguageChange = onLanguageChange,
                            guestLabel = strings.authContinueAsGuestAction,
                            isConfigured = AniListOAuthConfig.isConfigured,
                            configMissingLabel = strings.authConfigurationMissing,
                            onGuest = { guestMode = true },
                        )
                    }
                    AppRoute.SHELL -> {
                        Root(
                            settingsState = settingsState,
                            language = language,
                            onLanguageChange = onLanguageChange,
                            palette = palette,
                            onPaletteChange = {
                                palette = it
                                store.putString(SettingsStoreKeys.ThemePalette, paletteIdOf(it))
                            },
                            themeType = themeType,
                            onThemeTypeChange = {
                                themeType = it
                                store.putString(SettingsStoreKeys.ThemeMode, it.name)
                            },
                            font = font,
                            onFontChange = {
                                font = it
                                store.putString(SettingsStoreKeys.Font, it.name)
                            },
                            viewer = (current as? SessionState.Authenticated)?.viewer,
                            onLogin = { dependencies.oAuthLauncher.open() },
                            onLogout = {
                                guestMode = false
                                dependencies.logout()
                            },
                            animeListRepository = dependencies.animeListRepository,
                            mangaListRepository = dependencies.mangaListRepository,
                            mediaListEntryRepository = dependencies.mediaListEntryRepository,
                        )
                    }
                    AppRoute.ERROR -> {
                        val sessionError = (current as? SessionState.Error)?.error
                        val detail = sessionError?.let { strings.networkErrorMessage(it) }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            MessagePanel(
                                title = strings.errorUnknownLabel,
                                // Unknown maps to the same copy as the title; hiding
                                // it avoids showing the same line twice.
                                message = detail?.takeIf { it != strings.errorUnknownLabel },
                                icon = Icons.Filled.Warning,
                                actionLabel = strings.retryAction,
                                onAction = {
                                    scope.launch { dependencies.sessionManager.bootstrap() }
                                },
                            )
                            TextButton(
                                onClick = {
                                    guestMode = false
                                    dependencies.logout()
                                },
                            ) {
                                Text(
                                    text = strings.logoutAction,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Session routing key: drives the landing/shell transition, never the session itself. */
private enum class AppRoute {
    LOADING,
    LANDING,
    SHELL,
    ERROR,
}

/** AniList brand blue, raw hex (converted at the UI boundary). */
private const val ANILIST_BRAND_HEX = "#02A9FF"
