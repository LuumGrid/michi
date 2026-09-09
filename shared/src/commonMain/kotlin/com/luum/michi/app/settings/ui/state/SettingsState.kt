package com.luum.michi.app.settings.ui.state

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.storage.domain.SettingsStore
import com.luum.michi.app.core.storage.domain.SettingsStoreKeys
import com.luum.michi.app.settings.domain.model.SettingsData
import com.luum.michi.app.settings.domain.SettingsRepository
import com.luum.michi.app.settings.domain.model.DiscoverTabOption
import com.luum.michi.app.settings.domain.model.ListSort
import com.luum.michi.app.settings.domain.model.NotificationPreferences
import com.luum.michi.app.settings.domain.model.ScoreFormat
import com.luum.michi.app.settings.domain.model.ThemeMode
import com.luum.michi.app.settings.domain.model.TitleLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val KeyThemeMode = SettingsStoreKeys.ThemeMode
private const val KeyDefaultDiscoverTab = SettingsStoreKeys.DefaultDiscoverTab
private const val KeyTitleLanguage = "title_language"
private const val KeyDisplayAdultContent = "display_adult_content"
private const val KeyScoreFormat = "score_format"
private const val KeyListSort = "list_sort"
private const val KeySplitCompletedAnime = "split_completed_anime"
private const val KeySplitCompletedManga = "split_completed_manga"
private const val KeyAdvancedScoring = "advanced_scoring"
private const val KeyNotificationAiring = "notification_airing"
private const val KeyNotificationActivity = "notification_activity"
private const val KeyNotificationFollowing = "notification_following"
private const val KeyNotificationForum = "notification_forum"
private const val KeyNotificationMessages = "notification_messages"
private const val KeyNotificationMedia = "notification_media"

private val SaveDebounce = 600.milliseconds

internal class SettingsState(
    private val repository: SettingsRepository,
    private val store: SettingsStore,
    private val scope: CoroutineScope,
) {
    // Local-only prefs (no AniList equivalent) — persisted to the store but never synced.
    private var _themeMode = ThemeMode.SYSTEM
    var themeMode: ThemeMode
        get() = _themeMode
        set(value) {
            _themeMode = value
            store.putString(KeyThemeMode, value.name)
        }

    private var _defaultDiscoverTab = DiscoverTabOption.DISCOVER
    var defaultDiscoverTab: DiscoverTabOption
        get() = _defaultDiscoverTab
        set(value) {
            _defaultDiscoverTab = value
            store.putString(KeyDefaultDiscoverTab, value.name)
        }

    // AniList-synced prefs.
    private var _titleLanguage = TitleLanguage.ROMAJI
    var titleLanguage: TitleLanguage
        get() = _titleLanguage
        set(value) {
            _titleLanguage = value
            persistTitleLanguage(value)
            scheduleSave()
        }

    private var _displayAdultContent = false
    var displayAdultContent: Boolean
        get() = _displayAdultContent
        set(value) {
            _displayAdultContent = value
            store.putBoolean(KeyDisplayAdultContent, value)
            scheduleSave()
        }

    private var _scoreFormat = ScoreFormat.POINT_10_DECIMAL
    var scoreFormat: ScoreFormat
        get() = _scoreFormat
        set(value) {
            _scoreFormat = value
            persistScoreFormat(value)
            scheduleSave()
        }

    private var _listSort = ListSort.UPDATED
    var listSort: ListSort
        get() = _listSort
        set(value) {
            _listSort = value
            persistListSort(value)
            scheduleSave()
        }

    private var _splitCompletedAnime = true
    var splitCompletedAnime: Boolean
        get() = _splitCompletedAnime
        set(value) {
            _splitCompletedAnime = value
            store.putBoolean(KeySplitCompletedAnime, value)
            scheduleSave()
        }

    private var _splitCompletedManga = false
    var splitCompletedManga: Boolean
        get() = _splitCompletedManga
        set(value) {
            _splitCompletedManga = value
            store.putBoolean(KeySplitCompletedManga, value)
            scheduleSave()
        }

    private var _advancedScoring = false
    var advancedScoring: Boolean
        get() = _advancedScoring
        set(value) {
            _advancedScoring = value
            store.putBoolean(KeyAdvancedScoring, value)
            scheduleSave()
        }

    private var _notifications = NotificationPreferences()
    var notifications: NotificationPreferences
        get() = _notifications
        set(value) {
            _notifications = value
            persistNotifications(value)
            scheduleSave()
        }

    var error: NetworkError? = null
        private set

    var isSaving: Boolean = false
        private set

    private var saveJob: Job? = null

    init {
        hydrateFromStore()
        scope.launch {
            when (val result = repository.loadSettings()) {
                is NetworkResult.Success -> applyLoaded(result.value)
                is NetworkResult.Failure -> error = result.error
            }
        }
    }

    private fun hydrateFromStore() {
        store.getString(KeyThemeMode)?.let { saved ->
            ThemeMode.entries.firstOrNull { it.name == saved }?.let { _themeMode = it }
        }
        store.getString(KeyDefaultDiscoverTab)?.let { saved ->
            DiscoverTabOption.entries.firstOrNull { it.name == saved }?.let { _defaultDiscoverTab = it }
        }
        store.getString(KeyTitleLanguage)?.let { saved ->
            TitleLanguage.entries.firstOrNull { it.name == saved }?.let { _titleLanguage = it }
        }
        _displayAdultContent = store.getBoolean(KeyDisplayAdultContent, _displayAdultContent)
        store.getString(KeyScoreFormat)?.let { saved ->
            ScoreFormat.entries.firstOrNull { it.name == saved }?.let { _scoreFormat = it }
        }
        store.getString(KeyListSort)?.let { saved ->
            ListSort.entries.firstOrNull { it.name == saved }?.let { _listSort = it }
        }
        _splitCompletedAnime = store.getBoolean(KeySplitCompletedAnime, _splitCompletedAnime)
        _splitCompletedManga = store.getBoolean(KeySplitCompletedManga, _splitCompletedManga)
        _advancedScoring = store.getBoolean(KeyAdvancedScoring, _advancedScoring)
        _notifications = NotificationPreferences(
            airing = store.getBoolean(KeyNotificationAiring, _notifications.airing),
            activity = store.getBoolean(KeyNotificationActivity, _notifications.activity),
            following = store.getBoolean(KeyNotificationFollowing, _notifications.following),
            forum = store.getBoolean(KeyNotificationForum, _notifications.forum),
            messages = store.getBoolean(KeyNotificationMessages, _notifications.messages),
            media = store.getBoolean(KeyNotificationMedia, _notifications.media),
        )
    }

    /** Applies server-loaded values directly to the backing fields, bypassing the save scheduler. */
    private fun applyLoaded(data: SettingsData) {
        _titleLanguage = data.titleLanguage
        persistTitleLanguage(data.titleLanguage)
        _displayAdultContent = data.displayAdultContent
        store.putBoolean(KeyDisplayAdultContent, data.displayAdultContent)
        _scoreFormat = data.scoreFormat
        persistScoreFormat(data.scoreFormat)
        _listSort = data.listSort
        persistListSort(data.listSort)
        _splitCompletedAnime = data.splitCompletedAnime
        store.putBoolean(KeySplitCompletedAnime, data.splitCompletedAnime)
        _splitCompletedManga = data.splitCompletedManga
        store.putBoolean(KeySplitCompletedManga, data.splitCompletedManga)
        _advancedScoring = data.advancedScoring
        store.putBoolean(KeyAdvancedScoring, data.advancedScoring)
        _notifications = data.notifications
        persistNotifications(data.notifications)
        error = null
    }

    private fun persistTitleLanguage(value: TitleLanguage) {
        store.putString(KeyTitleLanguage, value.name)
    }

    private fun persistScoreFormat(value: ScoreFormat) {
        store.putString(KeyScoreFormat, value.name)
    }

    private fun persistListSort(value: ListSort) {
        store.putString(KeyListSort, value.name)
    }

    private fun persistNotifications(value: NotificationPreferences) {
        store.putBoolean(KeyNotificationAiring, value.airing)
        store.putBoolean(KeyNotificationActivity, value.activity)
        store.putBoolean(KeyNotificationFollowing, value.following)
        store.putBoolean(KeyNotificationForum, value.forum)
        store.putBoolean(KeyNotificationMessages, value.messages)
        store.putBoolean(KeyNotificationMedia, value.media)
    }

    private fun currentData(): SettingsData = SettingsData(
        titleLanguage = _titleLanguage,
        scoreFormat = _scoreFormat,
        displayAdultContent = _displayAdultContent,
        listSort = _listSort,
        splitCompletedAnime = _splitCompletedAnime,
        splitCompletedManga = _splitCompletedManga,
        advancedScoring = _advancedScoring,
        notifications = _notifications,
    )

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SaveDebounce)
            isSaving = true
            try {
                when (val result = repository.saveSettings(currentData())) {
                    is NetworkResult.Success -> error = null
                    is NetworkResult.Failure -> error = result.error
                }
            } finally {
                isSaving = false
            }
        }
    }
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createSettingsState(
    repository: SettingsRepository,
    store: SettingsStore,
    scope: CoroutineScope,
): SettingsState {
    return SettingsState(repository, store, scope)
}
