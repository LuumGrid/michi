package com.luum.michi.app.settings.domain.model

/**
 * AniList-synced settings payload. Theme and default discover tab are intentionally
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
