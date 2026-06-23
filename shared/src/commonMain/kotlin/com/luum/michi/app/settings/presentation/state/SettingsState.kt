package com.luum.michi.app.settings.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.NetworkError
import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.core.platform.PlatformSettingsStore
import com.luum.michi.app.core.platform.SettingsStoreKeys
import com.luum.michi.app.settings.data.SettingsData
import com.luum.michi.app.settings.data.SettingsRepository
import com.luum.michi.app.settings.presentation.model.HomeTabOption
import com.luum.michi.app.settings.presentation.model.ListSort
import com.luum.michi.app.settings.presentation.model.NotificationPreferences
import com.luum.michi.app.settings.presentation.model.ScoreFormat
import com.luum.michi.app.settings.presentation.model.ThemeMode
import com.luum.michi.app.settings.presentation.model.TitleLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val KeyThemeMode = SettingsStoreKeys.ThemeMode
private const val KeyDefaultHomeTab = SettingsStoreKeys.DefaultHomeTab
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
    private val store: PlatformSettingsStore,
    private val scope: CoroutineScope,
) {
    // Local-only prefs (no AniList equivalent) — persisted to the store but never synced.
    private var _themeMode by mutableStateOf(ThemeMode.SYSTEM)
    var themeMode: ThemeMode
        get() = _themeMode
        set(value) {
            _themeMode = value
            store.putString(KeyThemeMode, value.name)
        }

    private var _defaultHomeTab by mutableStateOf(HomeTabOption.HOME)
    var defaultHomeTab: HomeTabOption
        get() = _defaultHomeTab
        set(value) {
            _defaultHomeTab = value
            store.putString(KeyDefaultHomeTab, value.name)
        }

    // AniList-synced prefs.
    private var _titleLanguage by mutableStateOf(TitleLanguage.ROMAJI)
    var titleLanguage: TitleLanguage
        get() = _titleLanguage
        set(value) {
            _titleLanguage = value
            persistTitleLanguage(value)
            scheduleSave()
        }

    private var _displayAdultContent by mutableStateOf(false)
    var displayAdultContent: Boolean
        get() = _displayAdultContent
        set(value) {
            _displayAdultContent = value
            store.putBoolean(KeyDisplayAdultContent, value)
            scheduleSave()
        }

    private var _scoreFormat by mutableStateOf(ScoreFormat.POINT_10_DECIMAL)
    var scoreFormat: ScoreFormat
        get() = _scoreFormat
        set(value) {
            _scoreFormat = value
            persistScoreFormat(value)
            scheduleSave()
        }

    private var _listSort by mutableStateOf(ListSort.UPDATED)
    var listSort: ListSort
        get() = _listSort
        set(value) {
            _listSort = value
            persistListSort(value)
            scheduleSave()
        }

    private var _splitCompletedAnime by mutableStateOf(true)
    var splitCompletedAnime: Boolean
        get() = _splitCompletedAnime
        set(value) {
            _splitCompletedAnime = value
            store.putBoolean(KeySplitCompletedAnime, value)
            scheduleSave()
        }

    private var _splitCompletedManga by mutableStateOf(false)
    var splitCompletedManga: Boolean
        get() = _splitCompletedManga
        set(value) {
            _splitCompletedManga = value
            store.putBoolean(KeySplitCompletedManga, value)
            scheduleSave()
        }

    private var _advancedScoring by mutableStateOf(false)
    var advancedScoring: Boolean
        get() = _advancedScoring
        set(value) {
            _advancedScoring = value
            store.putBoolean(KeyAdvancedScoring, value)
            scheduleSave()
        }

    private var _notifications by mutableStateOf(NotificationPreferences())
    var notifications: NotificationPreferences
        get() = _notifications
        set(value) {
            _notifications = value
            persistNotifications(value)
            scheduleSave()
        }

    var error: NetworkError? = null
        private set

    var isSaving: Boolean by mutableStateOf(false)
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
        store.getString(KeyDefaultHomeTab)?.let { saved ->
            HomeTabOption.entries.firstOrNull { it.name == saved }?.let { _defaultHomeTab = it }
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

@Composable
internal fun rememberSettingsState(
    repository: SettingsRepository,
    store: PlatformSettingsStore,
): SettingsState {
    val scope = rememberCoroutineScope()
    return remember { SettingsState(repository, store, scope) }
}
