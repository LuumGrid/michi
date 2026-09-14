package com.luum.michi.app.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.language.domain.LanguageStrings
import com.luum.michi.app.ui.components.GhostButton
import com.luum.michi.app.ui.components.ModalSheet
import com.luum.michi.app.ui.components.OptionGroup
import com.luum.michi.app.ui.components.OptionRow
import com.luum.michi.app.ui.components.PrimaryButton
import com.luum.michi.app.ui.components.Toolbar
import com.luum.michi.app.ui.components.ToolbarAction
import com.luum.michi.app.ui.components.ToolbarNavigation
import com.luum.michi.app.ui.icons.AppIcons
import com.luum.michi.app.ui.theme.AppFont
import com.luum.michi.app.ui.theme.DefaultTheme
import com.luum.michi.app.ui.theme.OceanTheme
import com.luum.michi.app.ui.theme.SakuraTheme
import com.luum.michi.app.ui.theme.ThemeColors
import com.luum.michi.app.ui.theme.ThemeType
import com.luum.michi.app.ui.theme.brandColor
import com.luum.michi.app.ui.theme.displayName
import com.luum.michi.app.ui.theme.fontFamilyFor

/**
 * One login entry per service (AniList today; MAL and others tomorrow by
 * appending to [services]). Brand literal kept untranslated on purpose.
 * [brandHex] is a raw hex String converted at the UI boundary (project rule).
 */
internal data class AuthService(
    val id: String,
    val label: String,
    val onLogin: () -> Unit,
    val icon: ImageVector? = null,
    val brandHex: String? = null,
)

/**
 * IG-style landing: "Michi" toolbar with language + theme switches merged
 * in a single trailing capsule (our 2+ rule), brand mark, service buttons,
 * guest entry. Governed by the default theme like everything else.
 * Stateless except sheet visibility: selections are hoisted to `App`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthLandingScreen(
    strings: LanguageStrings,
    services: List<AuthService>,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    palette: ThemeColors,
    onPaletteChange: (ThemeColors) -> Unit,
    themeType: ThemeType,
    onThemeTypeChange: (ThemeType) -> Unit,
    font: AppFont,
    onFontChange: (AppFont) -> Unit,
    guestLabel: String,
    isConfigured: Boolean,
    configMissingLabel: String,
    onGuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var languageSheet by remember { mutableStateOf(false) }
    var themeSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Toolbar(
                title = "Michi",
                navigation = ToolbarNavigation.None,
                actions = listOf(
                    ToolbarAction(
                        id = ACTION_LANGUAGE,
                        icon = AppIcons.Language,
                        contentDescription = strings.languageLabel,
                        group = GROUP_AUTH_TOOLS,
                    ),
                    ToolbarAction(
                        id = ACTION_THEME,
                        icon = AppIcons.Theme,
                        contentDescription = strings.settingsThemeTitle,
                        group = GROUP_AUTH_TOOLS,
                    ),
                ),
                search = null,
                backContentDescription = null,
                clearContentDescription = null,
                onNavigation = {},
                onAction = { id ->
                    when (id) {
                        ACTION_LANGUAGE -> languageSheet = true
                        ACTION_THEME -> themeSheet = true
                    }
                },
                onSearchChange = {},
                onSearchSubmit = {},
                onSearchClose = {},
                onSearchClear = {},
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = strings.authWelcomeTitle,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.authWelcomeSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (isConfigured) {
                services.forEach { service ->
                    val brand = service.brandHex?.let { brandColor(it) }
                    PrimaryButton(
                        label = service.label,
                        onClick = service.onLogin,
                        leadingIcon = service.icon,
                        containerColor = brand,
                        contentColor = if (brand != null) Color.White else null,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = configMissingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            GhostButton(
                label = guestLabel,
                onClick = onGuest,
            )
        }
    }

    if (languageSheet) {
        ModalSheet(
            title = strings.languageLabel,
            dismissLabel = strings.dismissAction,
            onDismiss = { languageSheet = false },
        ) {
            OptionGroup(title = null) {
                AppLanguage.available.forEachIndexed { index, option ->
                    OptionRow(
                        label = option.displayName,
                        selected = option == language,
                    onClick = {
                        onLanguageChange(option)
                    },
                    divider = index > 0,
                    )
                }
            }
        }
    }

    if (themeSheet) {
        ModalSheet(
            title = strings.settingsThemeTitle,
            dismissLabel = strings.dismissAction,
            onDismiss = { themeSheet = false },
        ) {
            OptionGroup(title = strings.themePaletteSection) {
                ThemePalettes.forEachIndexed { index, (name, option) ->
                    OptionRow(
                        label = name,
                        selected = option::class == palette::class,
                    onClick = {
                        onPaletteChange(option)
                    },
                        divider = index > 0,
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(option.seed),
                            )
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OptionGroup(title = strings.themeModeSection) {
                ThemeType.entries.forEachIndexed { index, option ->
                    OptionRow(
                        label = option.label(strings),
                        selected = option == themeType,
                    onClick = {
                        onThemeTypeChange(option)
                    },
                        divider = index > 0,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OptionGroup(title = strings.themeFontSection) {
                AppFont.entries.forEachIndexed { index, option ->
                    OptionRow(
                        label = option.displayName,
                        selected = option == font,
                    onClick = {
                        onFontChange(option)
                    },
                        divider = index > 0,
                        leading = {
                            Text(
                                text = "Ag",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = fontFamilyFor(option),
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

private const val ACTION_LANGUAGE = "language"
private const val ACTION_THEME = "theme"

/** Shared capsule group for the auth switches (language + theme). */
private const val GROUP_AUTH_TOOLS = "auth-tools"

/** Brand palette names travel raw on purpose (like "Michi"): not copy. */
private val ThemePalettes: List<Pair<String, ThemeColors>> = listOf(
    "Default" to DefaultTheme(),
    "Ocean" to OceanTheme(),
    "Sakura" to SakuraTheme(),
)

private fun ThemeType.label(strings: LanguageStrings): String = when (this) {
    ThemeType.SYSTEM -> strings.settingsThemeSystem
    ThemeType.LIGHT -> strings.settingsThemeLight
    ThemeType.DARK -> strings.settingsThemeDark
}
