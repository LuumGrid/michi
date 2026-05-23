package com.luum.michi.app.settings.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luum.michi.app.settings.presentation.model.HomeTabOption
import com.luum.michi.app.settings.presentation.model.ListSort
import com.luum.michi.app.settings.presentation.model.NotificationPreferences
import com.luum.michi.app.settings.presentation.model.ScoreFormat
import com.luum.michi.app.settings.presentation.model.ThemeMode
import com.luum.michi.app.settings.presentation.model.TitleLanguage

internal class SettingsState {
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    var defaultHomeTab by mutableStateOf(HomeTabOption.HOME)
    var titleLanguage by mutableStateOf(TitleLanguage.ROMAJI)
    var displayAdultContent by mutableStateOf(false)
    var scoreFormat by mutableStateOf(ScoreFormat.POINT_10_DECIMAL)
    var listSort by mutableStateOf(ListSort.UPDATED)
    var splitCompletedAnime by mutableStateOf(true)
    var splitCompletedManga by mutableStateOf(false)
    var advancedScoring by mutableStateOf(false)
    var notifications by mutableStateOf(NotificationPreferences())
}

@Composable
internal fun rememberSettingsState(): SettingsState = remember { SettingsState() }
