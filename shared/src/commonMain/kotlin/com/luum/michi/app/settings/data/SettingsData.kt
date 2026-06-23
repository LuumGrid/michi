package com.luum.michi.app.settings.data

import com.luum.michi.app.settings.presentation.model.ListSort
import com.luum.michi.app.settings.presentation.model.NotificationPreferences
import com.luum.michi.app.settings.presentation.model.ScoreFormat
import com.luum.michi.app.settings.presentation.model.TitleLanguage

/**
 * AniList-synced settings payload. Theme and default home tab are intentionally
 * excluded - they are local-only preferences with no AniList equivalent.
 */
internal data class SettingsData(
    val titleLanguage: TitleLanguage,
    val scoreFormat: ScoreFormat,
    val displayAdultContent: Boolean,
    val listSort: ListSort,
    val splitCompletedAnime: Boolean,
    val splitCompletedManga: Boolean,
    val advancedScoring: Boolean,
    val notifications: NotificationPreferences,
)
