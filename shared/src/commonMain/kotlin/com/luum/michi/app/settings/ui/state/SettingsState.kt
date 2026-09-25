package com.luum.michi.app.settings.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.storage.domain.SettingsStore
import com.luum.michi.app.settings.domain.model.SettingsData
import com.luum.michi.app.settings.domain.SettingsRepository
import com.luum.michi.app.settings.domain.model.ListSort
import com.luum.michi.app.settings.domain.model.NotificationPreferences
import com.luum.michi.app.settings.domain.model.ScoreFormat
import com.luum.michi.app.settings.domain.model.StaffNameLanguage
import com.luum.michi.app.settings.domain.model.TitleLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val KeyTitleLanguage = "title_language"
private const val KeyStaffNameLanguage = "staff_name_language"
private const val KeyDisplayAdultContent = "display_adult_content"
private const val KeyScoreFormat = "score_format"
private const val KeyListSort = "list_sort"
private const val KeySplitCompletedAnime = "split_completed_anime"
private const val KeySplitCompletedManga = "split_completed_manga"
private const val KeyAdvancedScoring = "advanced_scoring"
private const val KeyPersistListSort = "persist_list_sort"
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
    val canSync: Boolean = true,
) {
    // AniList-synced prefs. Backed by state so the UI recomposes on
    // load/save; public setters persist locally and schedule the save.
    private val titleLanguageState = mutableStateOf(TitleLanguage.ROMAJI)
    var titleLanguage: TitleLanguage
        get() = titleLanguageState.value
        set(value) {
            titleLanguageState.value = value
            persistTitleLanguage(value)
            scheduleSave()
        }

    private val staffNameLanguageState = mutableStateOf(StaffNameLanguage.ROMAJI_WESTERN)
    var staffNameLanguage: StaffNameLanguage
        get() = staffNameLanguageState.value
        set(value) {
            staffNameLanguageState.value = value
            persistStaffNameLanguage(value)
            scheduleSave()
        }

    private val displayAdultContentState = mutableStateOf(false)
    var displayAdultContent: Boolean
        get() = displayAdultContentState.value
        set(value) {
            displayAdultContentState.value = value
            store.putBoolean(KeyDisplayAdultContent, value)
            scheduleSave()
        }

    private val scoreFormatState = mutableStateOf(ScoreFormat.POINT_10_DECIMAL)
    var scoreFormat: ScoreFormat
        get() = scoreFormatState.value
        set(value) {
            scoreFormatState.value = value
            persistScoreFormat(value)
            scheduleSave()
        }

    private val listSortState = mutableStateOf(ListSort.UPDATED)
    var listSort: ListSort
        get() = listSortState.value
        set(value) {
            listSortState.value = value
            saveListSort(value)
            scheduleSave()
        }

    private val splitCompletedAnimeState = mutableStateOf(true)
    var splitCompletedAnime: Boolean
        get() = splitCompletedAnimeState.value
        set(value) {
            splitCompletedAnimeState.value = value
            store.putBoolean(KeySplitCompletedAnime, value)
            scheduleSave()
        }

    private val splitCompletedMangaState = mutableStateOf(false)
    var splitCompletedManga: Boolean
        get() = splitCompletedMangaState.value
        set(value) {
            splitCompletedMangaState.value = value
            store.putBoolean(KeySplitCompletedManga, value)
            scheduleSave()
        }

    private val advancedScoringState = mutableStateOf(false)
    var advancedScoring: Boolean
        get() = advancedScoringState.value
        set(value) {
            advancedScoringState.value = value
            store.putBoolean(KeyAdvancedScoring, value)
            scheduleSave()
        }

    // Local-only: AniList has no field for sort memory, so this never
    // joins currentData()/scheduleSave — store only, like App theme/language.
    // ON by default: sort is set-and-forget, and non-changers save nothing
    // (fallback stays FOLLOW_LIST), so nobody is surprised either way.
    private val persistListSortState = mutableStateOf(true)
    var persistListSort: Boolean
        get() = persistListSortState.value
        set(value) {
            persistListSortState.value = value
            store.putBoolean(KeyPersistListSort, value)
        }

    private val notificationsState = mutableStateOf(NotificationPreferences())
    var notifications: NotificationPreferences
        get() = notificationsState.value
        set(value) {
            notificationsState.value = value
            persistNotifications(value)
            scheduleSave()
        }

    private val errorState = mutableStateOf<NetworkError?>(null)
    var error: NetworkError?
        get() = errorState.value
        private set(value) {
            errorState.value = value
        }

    private var saveJob: Job? = null

    /** True after the first successful load. Plain var: no UI reads it, only gating. */
    private var hasLoaded = false

    /** In-flight load, if any. Same Job idiom as saveJob. */
    private var loadJob: Job? = null

    init {
        hydrateFromStore()
    }

    /**
     * Loads the server values (and clears [error] on success).
     * No-op when [canSync] is false (guests never touch the network),
     * and skipped when already loaded or already loading unless [force].
     * Called from Settings' first open — a failed load retries itself
     * on the next open; an explicit retry passes force = true.
     */
    fun refresh(force: Boolean = false) {
        if (!canSync || (!force && hasLoaded)) return
        if (!force && loadJob?.isActive == true) return
        loadJob?.cancel()
        loadJob = scope.launch {
            when (val result = repository.loadSettings()) {
                is NetworkResult.Success -> {
                    applyLoaded(result.value)
                    hasLoaded = true
                }
                is NetworkResult.Failure -> error = result.error
            }
        }
    }

    private fun hydrateFromStore() {
        store.getString(KeyTitleLanguage)?.let { saved ->
            TitleLanguage.entries.firstOrNull { it.name == saved }?.let {
                titleLanguageState.value = it
            }
        }
        store.getString(KeyStaffNameLanguage)?.let { saved ->
            StaffNameLanguage.entries.firstOrNull { it.name == saved }?.let {
                staffNameLanguageState.value = it
            }
        }
        displayAdultContentState.value =
            store.getBoolean(KeyDisplayAdultContent, displayAdultContentState.value)
        store.getString(KeyScoreFormat)?.let { saved ->
            ScoreFormat.entries.firstOrNull { it.name == saved }?.let {
                scoreFormatState.value = it
            }
        }
        store.getString(KeyListSort)?.let { saved ->
            ListSort.entries.firstOrNull { it.name == saved }?.let { listSortState.value = it }
        }
        splitCompletedAnimeState.value =
            store.getBoolean(KeySplitCompletedAnime, splitCompletedAnimeState.value)
        splitCompletedMangaState.value =
            store.getBoolean(KeySplitCompletedManga, splitCompletedMangaState.value)
        advancedScoringState.value =
            store.getBoolean(KeyAdvancedScoring, advancedScoringState.value)
        persistListSortState.value =
            store.getBoolean(KeyPersistListSort, true)
        notificationsState.value = NotificationPreferences(
            airing = store.getBoolean(KeyNotificationAiring, notificationsState.value.airing),
            activity = store.getBoolean(KeyNotificationActivity, notificationsState.value.activity),
            following = store.getBoolean(KeyNotificationFollowing, notificationsState.value.following),
            forum = store.getBoolean(KeyNotificationForum, notificationsState.value.forum),
            messages = store.getBoolean(KeyNotificationMessages, notificationsState.value.messages),
            media = store.getBoolean(KeyNotificationMedia, notificationsState.value.media),
        )
    }

    /**
     * Applies server-loaded values directly to the backing states,
     * bypassing the save scheduler (what we just loaded must not be
     * sent straight back).
     */
    private fun applyLoaded(data: SettingsData) {
        titleLanguageState.value = data.titleLanguage
        persistTitleLanguage(data.titleLanguage)
        staffNameLanguageState.value = data.staffNameLanguage
        persistStaffNameLanguage(data.staffNameLanguage)
        displayAdultContentState.value = data.displayAdultContent
        store.putBoolean(KeyDisplayAdultContent, data.displayAdultContent)
        scoreFormatState.value = data.scoreFormat
        persistScoreFormat(data.scoreFormat)
        listSortState.value = data.listSort
        saveListSort(data.listSort)
        splitCompletedAnimeState.value = data.splitCompletedAnime
        store.putBoolean(KeySplitCompletedAnime, data.splitCompletedAnime)
        splitCompletedMangaState.value = data.splitCompletedManga
        store.putBoolean(KeySplitCompletedManga, data.splitCompletedManga)
        advancedScoringState.value = data.advancedScoring
        store.putBoolean(KeyAdvancedScoring, data.advancedScoring)
        notificationsState.value = data.notifications
        persistNotifications(data.notifications)
        error = null
    }

    private fun persistTitleLanguage(value: TitleLanguage) {
        store.putString(KeyTitleLanguage, value.name)
    }

    private fun persistStaffNameLanguage(value: StaffNameLanguage) {
        store.putString(KeyStaffNameLanguage, value.name)
    }

    private fun persistScoreFormat(value: ScoreFormat) {
        store.putString(KeyScoreFormat, value.name)
    }

    private fun saveListSort(value: ListSort) {
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
        titleLanguage = titleLanguage,
        staffNameLanguage = staffNameLanguage,
        scoreFormat = scoreFormat,
        displayAdultContent = displayAdultContent,
        listSort = listSort,
        splitCompletedAnime = splitCompletedAnime,
        splitCompletedManga = splitCompletedManga,
        advancedScoring = advancedScoring,
        notifications = notifications,
    )

    private fun scheduleSave() {
        if (!canSync) return
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SaveDebounce)
            when (val result = repository.saveSettings(currentData())) {
                is NetworkResult.Success -> error = null
                is NetworkResult.Failure -> error = result.error
            }
        }
    }
}

/**
 * Composition factory, keyed by [canSync] so login/logout recreates the
 * holder (different user, different data).
 */
@Composable
internal fun rememberSettingsState(
    repository: SettingsRepository,
    store: SettingsStore,
    scope: CoroutineScope,
    canSync: Boolean,
): SettingsState = remember(canSync) {
    SettingsState(repository, store, scope, canSync)
}

/** Pure-logic factory (placeholder for UI wiring). */
internal fun createSettingsState(
    repository: SettingsRepository,
    store: SettingsStore,
    scope: CoroutineScope,
    canSync: Boolean = true,
): SettingsState {
    return SettingsState(repository, store, scope, canSync)
}
