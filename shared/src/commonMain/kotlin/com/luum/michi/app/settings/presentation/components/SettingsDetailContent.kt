package com.luum.michi.app.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luum.michi.app.core.language.AppLanguage
import com.luum.michi.app.core.language.LanguageProvider
import com.luum.michi.app.core.platform.PlatformIcons
import com.luum.michi.app.core.platform.components.PlatformLanguageSwitch
import com.luum.michi.app.core.platform.components.PlatformThemeSwitch
import com.luum.michi.app.settings.presentation.model.SettingsItem
import com.luum.michi.app.settings.presentation.model.SettingsItemType

@Composable
internal fun SettingsDetailContent(
    item: SettingsItem,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
) {
    val strings = LanguageProvider.strings

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 112.dp,
            end = 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = item.title) {
            SettingsGroupHeader(text = item.title)
        }

        if (item.type == SettingsItemType.LANGUAGE) {
            item(key = "app-language") {
                SettingsRow(
                    item = SettingsItem(
                        title = strings.languageLabel,
                        subtitle = language.displayName,
                        icon = { PlatformIcons.Language },
                    ),
                    onClick = { },
                    trailingContent = {
                        PlatformLanguageSwitch(
                            selected = language,
                            onSelect = onLanguageChange,
                        )
                    },
                )
            }

            item(key = "subtitles") {
                SettingsRow(
                    item = SettingsItem(
                        title = strings.settingsSubtitlesTitle,
                        subtitle = strings.settingsSubtitlesSubtitle,
                        icon = { PlatformIcons.Accessibility },
                    ),
                    onClick = { },
                )
            }
        }

        if (item.type == SettingsItemType.ACCESSIBILITY) {
            item(key = "theme-mode") {
                SettingsRow(
                    item = SettingsItem(
                        title = strings.settingsDarkModeTitle,
                        subtitle = if (isDarkMode) {
                            strings.settingsDarkModeEnabledSubtitle
                        } else {
                            strings.settingsLightModeEnabledSubtitle
                        },
                        icon = { PlatformIcons.Accessibility },
                    ),
                    onClick = onToggleTheme,
                    trailingContent = {
                        PlatformThemeSwitch(
                            isDarkMode = isDarkMode,
                            onToggleTheme = onToggleTheme,
                        )
                    },
                )
            }
        }
    }
}
