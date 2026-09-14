package com.luum.michi.app

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
import com.luum.michi.app.root.AuthLandingScreen
import com.luum.michi.app.root.AuthService
import com.luum.michi.app.root.Root
import com.luum.michi.app.ui.components.MessagePanel
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.language.LocalStrings
import com.luum.michi.app.ui.theme.AppFont
import com.luum.michi.app.ui.theme.DefaultTheme
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
    var language by remember { mutableStateOf(AppLanguage.default) }
    var guestMode by remember { mutableStateOf(false) }
    var palette: ThemeColors by remember { mutableStateOf(DefaultTheme()) }
    var themeType by remember { mutableStateOf(ThemeType.SYSTEM) }
    var font by remember { mutableStateOf(AppFont.JAKARTA) }

    val strings = getLanguageStrings(language)
    val session by dependencies.sessionManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalStrings provides strings) {
        Theme(
            palette = palette,
            type = themeType,
            font = font,
        ) {
            when (val current = session) {
                SessionState.Loading -> {
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
                SessionState.Anonymous -> {
                    if (guestMode) {
                        Root()
                    } else {
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
                            onLanguageChange = { language = it },
                            palette = palette,
                            onPaletteChange = { palette = it },
                            themeType = themeType,
                            onThemeTypeChange = { themeType = it },
                            font = font,
                            onFontChange = { font = it },
                            guestLabel = strings.authContinueAsGuestAction,
                            isConfigured = AniListOAuthConfig.isConfigured,
                            configMissingLabel = strings.authConfigurationMissing,
                            onGuest = { guestMode = true },
                        )
                    }
                }
                is SessionState.Authenticated -> Root()
                is SessionState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        MessagePanel(
                            title = strings.errorUnknownLabel,
                            message = strings.networkErrorMessage(current.error),
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

/** AniList brand blue, raw hex (converted at the UI boundary). */
private const val ANILIST_BRAND_HEX = "#02A9FF"
