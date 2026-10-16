package com.luum.michi.app.settings.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.core.storage.domain.SettingsStore
import com.luum.michi.app.core.storage.domain.SettingsStoreKeys
import com.luum.michi.app.core.model.ImageQuality
import com.luum.michi.app.settings.domain.model.SettingsData
import com.luum.michi.app.settings.domain.SettingsRepository
import com.luum.michi.app.settings.domain.model.ListSort
import com.luum.michi.app.settings.domain.model.NotificationPreferences
import com.luum.michi.app.settings.domain.model.ScoreFormat
import com.luum.michi.app.settings.domain.model.StartCatalogOption
import com.luum.michi.app.settings.domain.model.StartTabOption
import com.luum.michi.app.settings.domain.model.StaffNameLanguage
import com.luum.michi.app.settings.domain.model.TitleLanguage
import com.luum.michi.app.settings.domain.model.parseScoreFormat
import com.luum.michi.app.settings.domain.model.parseTitleLanguage
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
private const val KeyAdvancedScoringAnimeNames = "advanced_scoring_anime_names"
private const val KeyAdvancedScoringMangaNames = "advanced_scoring_manga_names"
private const val KeyImageQuality = "image_quality"
private const val KeyPersistListSort = "persist_list_sort"
private const val KeyNotificationAiring = "notification_airing"
private const val KeyNotificationActivity = "notification_activity"
private const val KeyNotificationFollowing = "notification_following"
private const val KeyNotificationForum = "notification_forum"
private const val KeyNotificationMessages = "notification_messages"
private const val KeyNotificationMedia = "notification_media"

private val SaveDebounce = 600.milliseconds

private const val NAME_SEPARATOR = "\u001F"

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

    // Advanced-scoring category names per list type: server-owned but
    // store-persisted (like the toggle) so the editor section survives
    // restarts without opening Settings; refresh overwrites from server.
    // Encoded with the unit separator (never appears in category names).
    private val advancedScoringAnimeState = mutableStateOf(emptyList<String>())
    var advancedScoringAnime: List<String>
        get() = advancedScoringAnimeState.value
        private set(value) {
            advancedScoringAnimeState.value = value
        }

    private val advancedScoringMangaState = mutableStateOf(emptyList<String>())
    var advancedScoringManga: List<String>
        get() = advancedScoringMangaState.value
        private set(value) {
            advancedScoringMangaState.value = value
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

    // Local-only (never synced): how sharp the covers render.
    private val imageQualityState = mutableStateOf(ImageQuality.AUTO)
    var imageQuality: ImageQuality
        get() = imageQualityState.value
        set(value) {
            imageQualityState.value = value
            store.putString(KeyImageQuality, value.name)
        }

    // Local-only (never synced): where the shell opens and the dependent
    // sub-choice for that tab. The section ids are the feature enums' names —
    // settings never imports a feature; Root validates and resolves them (an
    // unknown/renamed id falls back to ALL).
    private val startTabState = mutableStateOf(StartTabOption.DISCOVER)
    var startTab: StartTabOption
        get() = startTabState.value
        set(value) {
            startTabState.value = value
            store.putString(SettingsStoreKeys.StartTab, value.name)
        }

    private val startCatalogTypeState = mutableStateOf(StartCatalogOption.ANIME)
    var startCatalogType: StartCatalogOption
        get() = startCatalogTypeState.value
        set(value) {
            startCatalogTypeState.value = value
            store.putString(SettingsStoreKeys.StartCatalogType, value.name)
        }

    private val startAnimeSectionState = mutableStateOf("ALL")
    var startAnimeSectionId: String
        get() = startAnimeSectionState.value
        set(value) {
            startAnimeSectionState.value = value
            store.putString(SettingsStoreKeys.StartAnimeSection, value)
        }

    private val startMangaSectionState = mutableStateOf("ALL")
    var startMangaSectionId: String
        get() = startMangaSectionState.value
        set(value) {
            startMangaSectionState.value = value
            store.putString(SettingsStoreKeys.StartMangaSection, value)
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

    private val refreshingState = mutableStateOf(false)
    var isRefreshing: Boolean
        get() = refreshingState.value
        private set(value) {
            refreshingState.value = value
        }

    private var saveJob: Job? = null

    /**
     * Bumps after an `UpdateUser` confirms a change to a setting that alters how
     * server content is localized (title language, staff name language). Root
     * reloads lists/dashboard/details/calendar off this counter — never off the
     * value change itself, which happens before the save lands.
     */
    var syncedGeneration: Int by mutableStateOf(0)
        private set

    // Last values confirmed by the server (load or save): the baseline the
    // generation compares against, so an edit only reloads once it is saved.
    private var lastSyncedTitleLanguage: TitleLanguage = TitleLanguage.ROMAJI
    private var lastSyncedStaffNameLanguage: StaffNameLanguage = StaffNameLanguage.ROMAJI_WESTERN

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
        isRefreshing = true
        // Snapshot before the request: a field the user edits while this load is
        // in flight is kept instead of being clobbered by the server value.
        val before = currentData()
        loadJob = scope.launch {
            try {
                when (val result = repository.loadSettings()) {
                    is NetworkResult.Success -> {
                        applyLoaded(result.value, before)
                        hasLoaded = true
                    }
                    is NetworkResult.Failure -> error = result.error
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    /**
     * Seeds the synced prefs from the freshly authenticated [Viewer] (which
     * carries title language, 18+ and score format) so the UI is correct before
     * [refresh] lands; the remaining synced values (split completed, etc.)
     * arrive with that refresh. Never schedules a save.
     */
    fun seedFrom(viewer: Viewer) {
        val title = parseTitleLanguage(viewer.titleLanguage)
        titleLanguageState.value = title
        persistTitleLanguage(title)
        displayAdultContentState.value = viewer.displayAdultContent
        store.putBoolean(KeyDisplayAdultContent, viewer.displayAdultContent)
        val score = parseScoreFormat(viewer.scoreFormat)
        scoreFormatState.value = score
        persistScoreFormat(score)
        lastSyncedTitleLanguage = title
    }

    /**
     * Drops the synced prefs back to defaults and clears their store keys, so a
     * guest never inherits the previous account's 18+ gate. Local-only prefs
     * (theme, language, start tab, image quality) are untouched. Called only on
     * a user → guest transition, never on a cold start as a guest.
     */
    fun clearSynced() {
        titleLanguageState.value = TitleLanguage.ROMAJI
        store.putString(KeyTitleLanguage, TitleLanguage.ROMAJI.name)
        staffNameLanguageState.value = StaffNameLanguage.ROMAJI_WESTERN
        store.putString(KeyStaffNameLanguage, StaffNameLanguage.ROMAJI_WESTERN.name)
        displayAdultContentState.value = false
        store.putBoolean(KeyDisplayAdultContent, false)
        scoreFormatState.value = ScoreFormat.POINT_10_DECIMAL
        store.putString(KeyScoreFormat, ScoreFormat.POINT_10_DECIMAL.name)
        listSortState.value = ListSort.UPDATED
        store.putString(KeyListSort, ListSort.UPDATED.name)
        splitCompletedAnimeState.value = true
        store.putBoolean(KeySplitCompletedAnime, true)
        splitCompletedMangaState.value = false
        store.putBoolean(KeySplitCompletedManga, false)
        advancedScoringState.value = false
        store.putBoolean(KeyAdvancedScoring, false)
        advancedScoringAnime = emptyList()
        store.putString(KeyAdvancedScoringAnimeNames, "")
        advancedScoringManga = emptyList()
        store.putString(KeyAdvancedScoringMangaNames, "")
        notificationsState.value = NotificationPreferences()
        persistNotifications(NotificationPreferences())
        lastSyncedTitleLanguage = TitleLanguage.ROMAJI
        lastSyncedStaffNameLanguage = StaffNameLanguage.ROMAJI_WESTERN
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
        advancedScoringAnime = decodeNames(store.getString(KeyAdvancedScoringAnimeNames))
        advancedScoringManga = decodeNames(store.getString(KeyAdvancedScoringMangaNames))
        persistListSortState.value =
            store.getBoolean(KeyPersistListSort, true)
        store.getString(KeyImageQuality)?.let { saved ->
            ImageQuality.entries.firstOrNull { it.name == saved }?.let {
                imageQualityState.value = it
            }
        }
        store.getString(SettingsStoreKeys.StartTab)?.let { saved ->
            StartTabOption.entries.firstOrNull { it.name == saved }?.let { startTabState.value = it }
        }
        store.getString(SettingsStoreKeys.StartCatalogType)?.let { saved ->
            StartCatalogOption.entries.firstOrNull { it.name == saved }?.let {
                startCatalogTypeState.value = it
            }
        }
        // Section ids are validated against the live section list by Root; a
        // stale/renamed id simply resolves to ALL there.
        store.getString(SettingsStoreKeys.StartAnimeSection)?.let { startAnimeSectionState.value = it }
        store.getString(SettingsStoreKeys.StartMangaSection)?.let { startMangaSectionState.value = it }
        notificationsState.value = NotificationPreferences(
            airing = store.getBoolean(KeyNotificationAiring, notificationsState.value.airing),
            activity = store.getBoolean(KeyNotificationActivity, notificationsState.value.activity),
            following = store.getBoolean(KeyNotificationFollowing, notificationsState.value.following),
            forum = store.getBoolean(KeyNotificationForum, notificationsState.value.forum),
            messages = store.getBoolean(KeyNotificationMessages, notificationsState.value.messages),
            media = store.getBoolean(KeyNotificationMedia, notificationsState.value.media),
        )
        // The hydrated values are the baseline until the server confirms: without
        // this, the first save would look like a change and force a false reload.
        lastSyncedTitleLanguage = titleLanguageState.value
        lastSyncedStaffNameLanguage = staffNameLanguageState.value
    }

    /**
     * Applies server-loaded values directly to the backing states,
     * bypassing the save scheduler (what we just loaded must not be
     * sent straight back). [before] is the data captured when the load
     * started: a field the user edited while the request was in flight
     * (`current != before`) is kept instead of being overwritten.
     */
    private fun applyLoaded(data: SettingsData, before: SettingsData? = null) {
        val title = keepEdited(titleLanguage, data.titleLanguage, before?.titleLanguage)
        titleLanguageState.value = title
        persistTitleLanguage(title)
        val staffName = keepEdited(staffNameLanguage, data.staffNameLanguage, before?.staffNameLanguage)
        staffNameLanguageState.value = staffName
        persistStaffNameLanguage(staffName)
        val adult = keepEdited(displayAdultContent, data.displayAdultContent, before?.displayAdultContent)
        displayAdultContentState.value = adult
        store.putBoolean(KeyDisplayAdultContent, adult)
        val score = keepEdited(scoreFormat, data.scoreFormat, before?.scoreFormat)
        scoreFormatState.value = score
        persistScoreFormat(score)
        val sort = keepEdited(listSort, data.listSort, before?.listSort)
        listSortState.value = sort
        saveListSort(sort)
        val splitAnime = keepEdited(splitCompletedAnime, data.splitCompletedAnime, before?.splitCompletedAnime)
        splitCompletedAnimeState.value = splitAnime
        store.putBoolean(KeySplitCompletedAnime, splitAnime)
        val splitManga = keepEdited(splitCompletedManga, data.splitCompletedManga, before?.splitCompletedManga)
        splitCompletedMangaState.value = splitManga
        store.putBoolean(KeySplitCompletedManga, splitManga)
        val adv = keepEdited(advancedScoring, data.advancedScoring, before?.advancedScoring)
        advancedScoringState.value = adv
        store.putBoolean(KeyAdvancedScoring, adv)
        val advAnime = keepEdited(advancedScoringAnime, data.advancedScoringAnime, before?.advancedScoringAnime)
        advancedScoringAnime = advAnime
        store.putString(KeyAdvancedScoringAnimeNames, encodeNames(advAnime))
        val advManga = keepEdited(advancedScoringManga, data.advancedScoringManga, before?.advancedScoringManga)
        advancedScoringManga = advManga
        store.putString(KeyAdvancedScoringMangaNames, encodeNames(advManga))
        val notifs = keepEdited(notifications, data.notifications, before?.notifications)
        notificationsState.value = notifs
        persistNotifications(notifs)
        // The baseline is what the SERVER sent, not the (possibly edited) value
        // kept above: an edit made during the load must still count as unsaved.
        lastSyncedTitleLanguage = data.titleLanguage
        lastSyncedStaffNameLanguage = data.staffNameLanguage
        error = null
    }

    /** Keeps the user's edit when it differs from the load-start value. */
    private fun <T> keepEdited(current: T, loaded: T, baseline: T?): T =
        if (baseline != null && current != baseline) current else loaded

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

    private fun encodeNames(names: List<String>): String =
        names.filter { it.isNotBlank() }.joinToString(NAME_SEPARATOR)

    private fun decodeNames(raw: String?): List<String> =
        raw?.split(NAME_SEPARATOR).orEmpty().filter { it.isNotBlank() }

    private fun persistNotifications(value: NotificationPreferences) {        store.putBoolean(KeyNotificationAiring, value.airing)
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
        advancedScoringAnime = advancedScoringAnime,
        advancedScoringManga = advancedScoringManga,
        notifications = notifications,
    )

    private fun scheduleSave() {
        if (!canSync) return
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SaveDebounce)
            val sent = currentData()
            when (val result = repository.saveSettings(sent)) {
                is NetworkResult.Success -> {
                    error = null
                    // Reload only when what we actually sent differs from the
                    // last server-confirmed values.
                    if (sent.titleLanguage != lastSyncedTitleLanguage ||
                        sent.staffNameLanguage != lastSyncedStaffNameLanguage
                    ) {
                        lastSyncedTitleLanguage = sent.titleLanguage
                        lastSyncedStaffNameLanguage = sent.staffNameLanguage
                        syncedGeneration += 1
                    }
                }
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
