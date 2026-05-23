package com.luum.michi.app.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.luum.michi.app.settings.presentation.model.HomeTabOption
import com.luum.michi.app.settings.presentation.model.ListSort
import com.luum.michi.app.settings.presentation.model.ScoreFormat
import com.luum.michi.app.settings.presentation.model.SettingsItem
import com.luum.michi.app.settings.presentation.model.SettingsItemType
import com.luum.michi.app.settings.presentation.model.ThemeMode
import com.luum.michi.app.settings.presentation.model.TitleLanguage
import com.luum.michi.app.settings.presentation.model.label
import com.luum.michi.app.settings.presentation.state.SettingsState

@Composable
internal fun SettingsDetailContent(
    item: SettingsItem,
    settingsState: SettingsState,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
) {
    val strings = LanguageProvider.strings
    val systemDark = isSystemInDarkTheme()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = item.title) {
            SettingsGroupHeader(text = item.title)
        }

        item(key = "detail-body") {
            when (item.type) {
                SettingsItemType.THEME -> SettingsRadioPicker(
                    options = ThemeMode.entries,
                    selected = settingsState.themeMode,
                    onSelect = { mode ->
                        settingsState.themeMode = mode
                        val targetDark = when (mode) {
                            ThemeMode.SYSTEM -> systemDark
                            ThemeMode.LIGHT -> false
                            ThemeMode.DARK -> true
                        }
                        if (targetDark != isDarkMode) onToggleTheme()
                    },
                    label = { it.label(strings) },
                )

                SettingsItemType.LANGUAGE -> SettingsRadioPicker(
                    options = AppLanguage.available,
                    selected = language,
                    onSelect = onLanguageChange,
                    label = { it.displayName },
                )

                SettingsItemType.HOME_TAB -> SettingsRadioPicker(
                    options = HomeTabOption.entries,
                    selected = settingsState.defaultHomeTab,
                    onSelect = { settingsState.defaultHomeTab = it },
                    label = { it.label(strings) },
                )

                SettingsItemType.TITLE_LANGUAGE -> SettingsRadioPicker(
                    options = TitleLanguage.entries,
                    selected = settingsState.titleLanguage,
                    onSelect = { settingsState.titleLanguage = it },
                    label = { it.label(strings) },
                )

                SettingsItemType.SCORE_FORMAT -> SettingsRadioPicker(
                    options = ScoreFormat.entries,
                    selected = settingsState.scoreFormat,
                    onSelect = { settingsState.scoreFormat = it },
                    label = { it.label(strings) },
                )

                SettingsItemType.LIST_SORT -> SettingsRadioPicker(
                    options = ListSort.entries,
                    selected = settingsState.listSort,
                    onSelect = { settingsState.listSort = it },
                    label = { it.label(strings) },
                )

                SettingsItemType.NOTIFICATIONS -> SettingsNotificationsDetail(
                    preferences = settingsState.notifications,
                    onChange = { settingsState.notifications = it },
                )

                SettingsItemType.ABOUT -> SettingsAboutDetail()

                else -> { }
            }
        }
    }
}
