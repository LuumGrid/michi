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
import com.luum.michi.app.core.language.domain.AppLanguage
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
import com.luum.michi.app.ui.state.LatestRequest
import com.luum.michi.app.ui.theme.AppFont
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.ThemeColors
import com.luum.michi.app.ui.theme.ThemeType
import com.luum.michi.app.ui.theme.paletteForId
import com.luum.michi.app.ui.theme.paletteIdOf
import com.luum.michi.app.ui.theme.paletteStyleForId
import com.luum.michi.app.ui.theme.paletteStyleIdOf
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Store keys only SettingsState reads. The shell's startup keys
 * (`SettingsStoreKeys.Start*`) stay in core because Root reads them; these never
 * leave settings.
 */
private object SettingsPrefKeys {
    const val TitleLanguage = "title_language"
    const val StaffNameLanguage = "staff_name_language"
    const val DisplayAdultContent = "display_adult_content"
    const val ScoreFormat = "score_format"
    const val ListSort = "list_sort"
    const val SplitCompletedAnime = "split_completed_anime"
    const val SplitCompletedManga = "split_completed_manga"
    const val AdvancedScoring = "advanced_scoring"
    const val AdvancedScoringAnimeNames = "advanced_scoring_anime_names"
    const val AdvancedScoringMangaNames = "advanced_scoring_manga_names"
    const val ImageQuality = "image_quality"
    const val PersistListSort = "persist_list_sort"
    const val NotificationAiring = "notification_airing"
    const val NotificationActivity = "notification_activity"
    const val NotificationFollowing = "notification_following"
    const val NotificationForum = "notification_forum"
    const val NotificationMessages = "notification_messages"
    const val NotificationMedia = "notification_media"
}

private val SaveDebounce = 600.milliseconds

internal class SettingsState(
    private val repository: SettingsRepository,
    private val store: SettingsStore,
    private val scope: CoroutineScope,
    val canSync: Boolean = true,
) {
    // ── Synced pref factories: the public setter schedules the AniList save. ──
    private fun syncedBoolean(key: String, default: Boolean) =
        booleanPref(key, default, store) { scheduleSave() }

    private fun <E : Enum<E>> syncedEnum(key: String, default: E, values: List<E>) =
        enumPref(key, default, values, store) { scheduleSave() }

    private fun syncedStringList(key: String) =
        stringListPref(key, store) { scheduleSave() }

    private fun syncedNotifications(keys: NotificationKeys) =
        notificationsStoredPref(keys, store) { scheduleSave() }

    // ── Local pref factories: store only, never saved to AniList. ──
    private fun localBoolean(key: String, default: Boolean) =
        booleanPref(key, default, store)

    private fun <E : Enum<E>> localEnum(key: String, default: E, values: List<E>) =
        enumPref(key, default, values, store)

    private fun localString(key: String, default: String) =
        stringPref(key, default, store)

    // ── AniList-synced prefs. Backed by state so the UI recomposes on
    //    load/save; public setters persist locally and schedule the save. ──
    private val titleLanguagePref =
        syncedEnum(SettingsPrefKeys.TitleLanguage, TitleLanguage.ROMAJI, TitleLanguage.entries)
    var titleLanguage: TitleLanguage by titleLanguagePref

    private val staffNameLanguagePref =
        syncedEnum(SettingsPrefKeys.StaffNameLanguage, StaffNameLanguage.ROMAJI_WESTERN, StaffNameLanguage.entries)
    var staffNameLanguage: StaffNameLanguage by staffNameLanguagePref

    private val displayAdultContentPref =
        syncedBoolean(SettingsPrefKeys.DisplayAdultContent, false)
    var displayAdultContent: Boolean by displayAdultContentPref

    private val scoreFormatPref =
        syncedEnum(SettingsPrefKeys.ScoreFormat, ScoreFormat.POINT_10_DECIMAL, ScoreFormat.entries)
    var scoreFormat: ScoreFormat by scoreFormatPref

    private val listSortPref =
        syncedEnum(SettingsPrefKeys.ListSort, ListSort.UPDATED, ListSort.entries)
    var listSort: ListSort by listSortPref

    private val splitCompletedAnimePref =
        syncedBoolean(SettingsPrefKeys.SplitCompletedAnime, true)
    var splitCompletedAnime: Boolean by splitCompletedAnimePref

    private val splitCompletedMangaPref =
        syncedBoolean(SettingsPrefKeys.SplitCompletedManga, false)
    var splitCompletedManga: Boolean by splitCompletedMangaPref

    private val advancedScoringPref =
        syncedBoolean(SettingsPrefKeys.AdvancedScoring, false)
    var advancedScoring: Boolean by advancedScoringPref

    // Advanced-scoring category names per list type: server-owned but
    // store-persisted (like the toggle) so the editor section survives
    // restarts without opening Settings; refresh overwrites from server.
    // Read-only outside: only the pref writes them (hydrate/reset/applyLoaded).
    private val advancedScoringAnimePref =
        syncedStringList(SettingsPrefKeys.AdvancedScoringAnimeNames)
    val advancedScoringAnime: List<String> get() = advancedScoringAnimePref.value

    private val advancedScoringMangaPref =
        syncedStringList(SettingsPrefKeys.AdvancedScoringMangaNames)
    val advancedScoringManga: List<String> get() = advancedScoringMangaPref.value

    private val notificationsPref = syncedNotifications(
        NotificationKeys(
            airing = SettingsPrefKeys.NotificationAiring,
            activity = SettingsPrefKeys.NotificationActivity,
            following = SettingsPrefKeys.NotificationFollowing,
            forum = SettingsPrefKeys.NotificationForum,
            messages = SettingsPrefKeys.NotificationMessages,
            media = SettingsPrefKeys.NotificationMedia,
        ),
    )
    var notifications: NotificationPreferences by notificationsPref

    // ── Local-only prefs: AniList has no field for these, so they never join
    //    currentData()/scheduleSave — store only. ──

    // Sort memory. ON by default: sort is set-and-forget, and non-changers
    // save nothing (fallback stays FOLLOW_LIST), so nobody is surprised.
    private val persistListSortPref = localBoolean(SettingsPrefKeys.PersistListSort, true)
    var persistListSort: Boolean by persistListSortPref

    // How sharp the covers render.
    private val imageQualityPref =
        localEnum(SettingsPrefKeys.ImageQuality, ImageQuality.AUTO, ImageQuality.entries)
    var imageQuality: ImageQuality by imageQualityPref

    // App appearance (local-only, same store keys App used to read): language,
    // palette, color style, theme mode, font and surfaces. Never synced.
    private val languagePref =
        codedPref(
            key = SettingsStoreKeys.Language,
            store = store,
            decode = AppLanguage::fromCode,
            encode = { it.code },
        )
    var language: AppLanguage by languagePref

    private val palettePref =
        codedPref(SettingsStoreKeys.ThemePalette, store, ::paletteForId, ::paletteIdOf)
    var palette: ThemeColors by palettePref

    private val themeStylePref =
        codedPref(SettingsStoreKeys.ThemeStyle, store, ::paletteStyleForId, ::paletteStyleIdOf)
    var themeStyle: PaletteStyle by themeStylePref

    private val themeTypePref =
        localEnum(SettingsStoreKeys.ThemeMode, ThemeType.SYSTEM, ThemeType.entries)
    var themeType: ThemeType by themeTypePref

    private val fontPref =
        localEnum(SettingsStoreKeys.Font, AppFont.JAKARTA, AppFont.entries)
    var font: AppFont by fontPref

    private val surfacesPref =
        localEnum(SettingsStoreKeys.SurfaceStyle, SurfaceStyle.GLASS, SurfaceStyle.entries)
    var surfaces: SurfaceStyle by surfacesPref

    // "Pestaña al abrir": where the shell opens and the dependent sub-choice for
    // that tab. The section ids are the feature enums' names — settings never
    // imports a feature; Root validates and resolves them (an unknown/renamed
    // id falls back to ALL).
    private val startTabPref =
        localEnum(SettingsStoreKeys.StartTab, StartTabOption.DISCOVER, StartTabOption.entries)
    var startTab: StartTabOption by startTabPref

    private val startCatalogTypePref =
        localEnum(SettingsStoreKeys.StartCatalogType, StartCatalogOption.ANIME, StartCatalogOption.entries)
    var startCatalogType: StartCatalogOption by startCatalogTypePref

    private val startAnimeSectionPref = localString(SettingsStoreKeys.StartAnimeSection, "ALL")
    var startAnimeSectionId: String by startAnimeSectionPref

    private val startMangaSectionPref = localString(SettingsStoreKeys.StartMangaSection, "ALL")
    var startMangaSectionId: String by startMangaSectionPref

    /**
     * Sub-choice of the active start tab: DISCOVER parses a [StartCatalogOption]
     * name (unknown = no-op), ANIME/MANGA store the raw section id, PROFILE has
     * none. Root supplies the localized options; the id is validated when Root
     * resolves the startup state.
     */
    fun setStartSection(value: String) {
        when (startTab) {
            StartTabOption.DISCOVER -> StartCatalogOption.entries.firstOrNull { it.name == value }
                ?.let { startCatalogType = it }
            StartTabOption.ANIME -> startAnimeSectionId = value
            StartTabOption.MANGA -> startMangaSectionId = value
            StartTabOption.PROFILE -> Unit
        }
    }

    /** Every pref (hydrated together); synced ones also reset together. */
    private val allPrefs: List<StoredPref<*>> = listOf(
        titleLanguagePref,
        staffNameLanguagePref,
        displayAdultContentPref,
        scoreFormatPref,
        listSortPref,
        splitCompletedAnimePref,
        splitCompletedMangaPref,
        advancedScoringPref,
        advancedScoringAnimePref,
        advancedScoringMangaPref,
        notificationsPref,
        persistListSortPref,
        imageQualityPref,
        languagePref,
        palettePref,
        themeStylePref,
        themeTypePref,
        fontPref,
        surfacesPref,
        startTabPref,
        startCatalogTypePref,
        startAnimeSectionPref,
        startMangaSectionPref,
    )
    private val syncedPrefs: List<StoredPref<*>> = listOf(
        titleLanguagePref,
        staffNameLanguagePref,
        displayAdultContentPref,
        scoreFormatPref,
        listSortPref,
        splitCompletedAnimePref,
        splitCompletedMangaPref,
        advancedScoringPref,
        advancedScoringAnimePref,
        advancedScoringMangaPref,
        notificationsPref,
    )

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

    /** In-flight load, if any. */
    private val loadRequest = LatestRequest()

    // Runs after every pref + list above, so hydrateFromStore() sees them all.
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
        if (!force && loadRequest.isActive) return
        isRefreshing = true
        // Snapshot before the request: a field the user edits while this load is
        // in flight is kept instead of being clobbered by the server value.
        val before = currentData()
        // Only the current request settles or writes: a superseded load can never
        // stop the spinner of the load now in flight (nor write a stale snapshot).
        loadRequest.launch(
            scope = scope,
            onSettled = { isRefreshing = false },
        ) { isCurrent ->
            when (val result = repository.loadSettings()) {
                is NetworkResult.Success -> if (isCurrent()) {
                    applyLoaded(result.value, before)
                    hasLoaded = true
                }
                is NetworkResult.Failure -> if (isCurrent()) {
                    error = result.error
                }
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
        titleLanguagePref.setSilently(title)
        displayAdultContentPref.setSilently(viewer.displayAdultContent)
        scoreFormatPref.setSilently(parseScoreFormat(viewer.scoreFormat))
        lastSyncedTitleLanguage = title
    }

    /**
     * Drops the synced prefs back to defaults and writes those defaults to the
     * store, so a guest never inherits the previous account's 18+ gate. Local
     * prefs (start tab, image quality, persist list sort) are untouched. Called
     * only on a user → guest transition, never on a cold start as a guest.
     */
    fun clearSynced() {
        syncedPrefs.forEach { it.reset() }
        // reset() restored the defaults, so the state is the new baseline.
        lastSyncedTitleLanguage = titleLanguage
        lastSyncedStaffNameLanguage = staffNameLanguage
    }

    private fun hydrateFromStore() {
        allPrefs.forEach { it.hydrate() }
        // The hydrated values are the baseline until the server confirms: without
        // this, the first save would look like a change and force a false reload.
        lastSyncedTitleLanguage = titleLanguage
        lastSyncedStaffNameLanguage = staffNameLanguage
    }

    /**
     * Applies server-loaded values directly to the backing states,
     * bypassing the save scheduler (what we just loaded must not be
     * sent straight back). [before] is the data captured when the load
     * started: a field the user edited while the request was in flight
     * (`current != before`) is kept instead of being overwritten.
     */
    private fun applyLoaded(data: SettingsData, before: SettingsData? = null) {
        titleLanguagePref.applyLoaded(data.titleLanguage, before?.titleLanguage)
        staffNameLanguagePref.applyLoaded(data.staffNameLanguage, before?.staffNameLanguage)
        displayAdultContentPref.applyLoaded(data.displayAdultContent, before?.displayAdultContent)
        scoreFormatPref.applyLoaded(data.scoreFormat, before?.scoreFormat)
        listSortPref.applyLoaded(data.listSort, before?.listSort)
        splitCompletedAnimePref.applyLoaded(data.splitCompletedAnime, before?.splitCompletedAnime)
        splitCompletedMangaPref.applyLoaded(data.splitCompletedManga, before?.splitCompletedManga)
        advancedScoringPref.applyLoaded(data.advancedScoring, before?.advancedScoring)
        advancedScoringAnimePref.applyLoaded(data.advancedScoringAnime, before?.advancedScoringAnime)
        advancedScoringMangaPref.applyLoaded(data.advancedScoringManga, before?.advancedScoringManga)
        notificationsPref.applyLoaded(data.notifications, before?.notifications)
        // The baseline is what the SERVER sent, not the (possibly edited) value
        // kept above: an edit made during the load must still count as unsaved.
        lastSyncedTitleLanguage = data.titleLanguage
        lastSyncedStaffNameLanguage = data.staffNameLanguage
        error = null
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
