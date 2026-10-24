package com.luum.michi.app.settings

import com.luum.michi.app.DeferredDispatcher
import com.luum.michi.app.FakeGraphQL
import com.luum.michi.app.core.language.domain.AppLanguage
import com.luum.michi.app.core.model.ImageQuality
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.storage.domain.SettingsStoreKeys
import com.luum.michi.app.settings.domain.model.ListSort
import com.luum.michi.app.settings.domain.model.NotificationPreferences
import com.luum.michi.app.core.medialist.domain.ScoreFormat
import com.luum.michi.app.settings.domain.model.StaffNameLanguage
import com.luum.michi.app.settings.domain.model.StartTabOption
import com.luum.michi.app.settings.domain.model.TitleLanguage
import com.luum.michi.app.settings.repository.SettingsRepositoryImpl
import com.luum.michi.app.settings.ui.state.SettingsState
import com.luum.michi.app.ui.theme.AppFont
import com.luum.michi.app.ui.theme.AppPalette
import com.luum.michi.app.ui.theme.AppPaletteStyle
import com.luum.michi.app.ui.theme.SurfaceStyle
import com.luum.michi.app.ui.theme.ThemeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun notificationOption(type: String, enabled: Boolean) = buildJsonObject {
    put("type", type)
    put("enabled", enabled)
}

/** Distinctive Viewer payload: exercises precedence (airing flag over bucket),
 * the bucket any-logic (mixed activity types) and every list mapping. */
private val viewerPage = buildJsonObject {
    put("Viewer", buildJsonObject {
        put("options", buildJsonObject {
            put("titleLanguage", "ENGLISH")
            put("staffNameLanguage", "NATIVE")
            put("displayAdultContent", true)
            put("airingNotifications", true)
            putJsonArray("notificationOptions") {
                add(notificationOption("AIRING", false))
                add(notificationOption("ACTIVITY_LIKE", true))
                add(notificationOption("ACTIVITY_REPLY", false))
                add(notificationOption("ACTIVITY_MESSAGE", true))
                add(notificationOption("MEDIA_DATA_CHANGE", false))
                add(notificationOption("MEDIA_MERGE", false))
            }
        })
        put("mediaListOptions", buildJsonObject {
            put("scoreFormat", "POINT_100")
            put("rowOrder", "score")
            put("animeList", buildJsonObject {
                put("splitCompletedSectionByFormat", false)
                put("advancedScoringEnabled", true)
                putJsonArray("advancedScoring") {
                    add(JsonPrimitive("Story"))
                    add(JsonPrimitive("Animation"))
                }
            })
            put("mangaList", buildJsonObject {
                put("splitCompletedSectionByFormat", true)
                putJsonArray("advancedScoring") {
                    add(JsonPrimitive("Art"))
                }
            })
        })
    })
}

private val emptyUpdatePage = buildJsonObject {
    put("UpdateUser", buildJsonObject { put("id", 1) })
}

class SettingsIntegrationTest {

    private lateinit var scope: CoroutineScope

    private fun setUp() {
        scope = CoroutineScope(Dispatchers.Unconfined)
    }

    private fun tearDown() {
        scope.cancel()
    }

    private fun stateWith(
        client: ScriptedSettingsGraphQL,
        store: FakeSettingsStore = FakeSettingsStore(),
    ): SettingsState = SettingsState(
        repository = SettingsRepositoryImpl(client),
        store = store,
        scope = scope,
    )

    @Test
    fun loadAppliesServerValuesAndCachesLocally() {
        setUp()
        try {
            val client = ScriptedSettingsGraphQL(listOf(viewerPage))
            val store = FakeSettingsStore()
            val state = stateWith(client, store)

            state.refresh()

            assertEquals(TitleLanguage.ENGLISH, state.titleLanguage)
            assertEquals(StaffNameLanguage.NATIVE, state.staffNameLanguage)
            assertEquals(true, state.displayAdultContent)
            assertEquals(ScoreFormat.POINT_100, state.scoreFormat)
            assertEquals(ListSort.SCORE, state.listSort)
            assertEquals(false, state.splitCompletedAnime)
            assertEquals(true, state.splitCompletedManga)
            assertEquals(true, state.advancedScoring)
            assertEquals(listOf("Story", "Animation"), state.advancedScoringAnime)
            assertEquals(listOf("Art"), state.advancedScoringManga)
            // airingNotifications flag wins over the disabled AIRING bucket entry.
            assertEquals(true, state.notifications.airing)
            // Any-logic: mixed activity types resolve to true.
            assertEquals(true, state.notifications.activity)
            assertEquals(true, state.notifications.messages)
            assertEquals(false, state.notifications.media)
            assertEquals(1, client.calls)
            assertEquals("ENGLISH", store.getString("title_language"))
            assertEquals("POINT_100", store.getString("score_format"))
            assertEquals(true, store.getBoolean("display_adult_content", false))
        } finally {
            tearDown()
        }
    }

    @Test
    fun explicitNullAdvancedScoringNamesDecodeToEmpty() {        setUp()
        try {
            val page = buildJsonObject {
                put("Viewer", buildJsonObject {
                    put("mediaListOptions", buildJsonObject {
                        put("animeList", buildJsonObject {
                            put("advancedScoringEnabled", false)
                            put("advancedScoring", JsonNull)
                        })
                        put("mangaList", buildJsonObject {
                            put("advancedScoring", JsonNull)
                        })
                    })
                })
            }
            val state = stateWith(ScriptedSettingsGraphQL(listOf(page)))

            state.refresh()

            assertEquals(false, state.advancedScoring)
            assertEquals(emptyList(), state.advancedScoringAnime)
            assertEquals(emptyList(), state.advancedScoringManga)
        } finally {
            tearDown()
        }
    }

    @Test
    fun persistListSortRoundTripsThroughStoreOnly() {
        setUp()
        try {
            val store = FakeSettingsStore()
            val state = stateWith(ScriptedSettingsGraphQL(listOf(viewerPage)), store)

            // ON by default on fresh installs.
            assertEquals(true, state.persistListSort)
            state.persistListSort = false
            assertEquals(false, store.getBoolean("persist_list_sort", true))

            val revived = stateWith(ScriptedSettingsGraphQL(listOf(viewerPage)), store)
            assertEquals(false, revived.persistListSort)
        } finally {
            tearDown()
        }
    }

    @Test
    fun saveSendsMappedUpdateUserVariables(): Unit = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val client = ScriptedSettingsGraphQL(listOf(viewerPage, emptyUpdatePage))
            val state = SettingsState(
                repository = SettingsRepositoryImpl(client),
                store = FakeSettingsStore(),
                scope = testScope,
            )
            state.refresh()

            state.scoreFormat = ScoreFormat.POINT_5_STARS
            state.staffNameLanguage = StaffNameLanguage.ROMAJI
            state.listSort = ListSort.UPDATED
            state.splitCompletedAnime = false
            state.notifications = state.notifications.copy(airing = false)

            // Real debounce window (no coroutines-test in deps), isolated to this test.
            delay(1_200)

            assertEquals(2, client.calls)
            val mutation = client.requests.last()
            assertEquals("UpdateUser", mutation.operationName)
            val variables = mutation.variables
            assertEquals("POINT_5", variables?.get("scoreFormat")?.jsonPrimitive?.content)
            assertEquals("ROMAJI", variables?.get("staffNameLanguage")?.jsonPrimitive?.content)
            assertEquals("updatedAt", variables?.get("rowOrder")?.jsonPrimitive?.content)
            assertEquals(false, variables?.get("splitCompletedAnime")?.jsonPrimitive?.content.toBoolean())
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun aSaveReloadsOnlyWhenTheLanguageActuallyChanged(): Unit = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val client = ScriptedSettingsGraphQL(
                listOf(viewerPage, emptyUpdatePage, emptyUpdatePage, emptyUpdatePage),
            )
            val state = SettingsState(
                repository = SettingsRepositoryImpl(client),
                store = FakeSettingsStore(),
                scope = testScope,
            )
            state.refresh() // server: title ENGLISH, staff NATIVE
            assertEquals(0, state.syncedGeneration)

            // A non-language save does not reload.
            state.splitCompletedAnime = true
            delay(1_200)
            assertEquals(0, state.syncedGeneration, "a non-language save must not reload")

            // Changing the title language bumps it once the save is confirmed.
            state.titleLanguage = TitleLanguage.NATIVE
            delay(1_200)
            assertEquals(1, state.syncedGeneration)

            // Re-saving the same confirmed value does not bump again.
            state.listSort = ListSort.TITLE
            delay(1_200)
            assertEquals(1, state.syncedGeneration)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun aColdStartSaveDoesNotReloadWhenTheLanguageIsUnchanged(): Unit = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        try {
            // No refresh: the hydrated store value is the baseline.
            val store = FakeSettingsStore().apply { putString("title_language", "ENGLISH") }
            val state = SettingsState(
                repository = SettingsRepositoryImpl(ScriptedSettingsGraphQL(listOf(emptyUpdatePage))),
                store = store,
                scope = testScope,
            )
            assertEquals(0, state.syncedGeneration)

            state.listSort = ListSort.TITLE
            delay(1_200)

            assertEquals(0, state.syncedGeneration, "the hydrated baseline avoids a false reload")
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun loadFailureSetsErrorAndKeepsDefaults() {
        setUp()
        try {
            val client = ScriptedSettingsGraphQL(listOf(viewerPage), failureAt = 0)
            val store = FakeSettingsStore()
            val state = stateWith(client, store)

            state.refresh()

            assertIs<NetworkError.Http>(state.error)
            assertEquals(TitleLanguage.ROMAJI, state.titleLanguage)
            assertEquals(ScoreFormat.POINT_10_DECIMAL, state.scoreFormat)
            assertEquals(null, store.getString("title_language"))
            assertTrue(client.calls == 1)
        } finally {
            tearDown()
        }
    }

    @Test
    fun refreshClearsRefreshingOnSuccessAndRefiresOnForce() {
        setUp()
        try {
            val client = ScriptedSettingsGraphQL(listOf(viewerPage, viewerPage))
            val state = stateWith(client)

            state.refresh()
            assertEquals(false, state.isRefreshing)
            assertEquals(1, client.calls)

            // Second plain refresh is deduped; force re-fires.
            state.refresh()
            assertEquals(1, client.calls)
            state.refresh(force = true)
            assertEquals(false, state.isRefreshing)
            assertEquals(2, client.calls)
        } finally {
            tearDown()
        }
    }
    @Test
    fun refreshClearsRefreshingOnFailure() {
        setUp()
        try {
            val client = ScriptedSettingsGraphQL(listOf(viewerPage), failureAt = 0)
            val state = stateWith(client)

            state.refresh()

            assertIs<NetworkError.Http>(state.error)
            assertEquals(false, state.isRefreshing)
        } finally {
            tearDown()
        }
    }

    /**
     * A forced refresh cancels the load in flight; the cancelled load's `finally`
     * must not turn off the spinner of the new one. The [DeferredDispatcher]
     * orders the cancellation and the new dispatch deterministically.
     */
    @Test
    fun forcedRefreshKeepsTheSpinnerUntilTheNewLoadFinishes() {
        val dispatcher = DeferredDispatcher()
        val scope = CoroutineScope(dispatcher)
        try {
            val client = FakeGraphQL(listOf(viewerPage, viewerPage)).apply {
                gate = CompletableDeferred()
            }
            val state = SettingsState(
                repository = SettingsRepositoryImpl(client),
                store = FakeSettingsStore(),
                scope = scope,
            )

            state.refresh()
            dispatcher.runAll()
            assertTrue(state.isRefreshing, "the first load owns the spinner")

            state.refresh(force = true)
            dispatcher.runAll()
            assertTrue(
                state.isRefreshing,
                "a cancelled load must not stop the spinner of the load now in flight",
            )

            client.gate!!.complete(Unit)
            dispatcher.runAll()

            assertEquals(false, state.isRefreshing)
            assertEquals(2, client.calls)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun advancedNamesHydrateFromStoreWithoutNetwork() {
        setUp()
        try {
            val store = FakeSettingsStore()
            store.preload("advanced_scoring", true)
            store.preload("advanced_scoring_anime_names", "Story\u001FAnimation")
            val state = stateWith(ScriptedSettingsGraphQL(emptyList()), store)

            assertEquals(true, state.advancedScoring)
            assertEquals(listOf("Story", "Animation"), state.advancedScoringAnime)
            assertEquals(emptyList(), state.advancedScoringManga)
        } finally {
            tearDown()
        }
    }

    @Test
    fun refreshOverwritesPersistedNamesFromServer() {
        setUp()
        try {
            val store = FakeSettingsStore()
            store.preload("advanced_scoring_anime_names", "Stale\u001FNames")
            val client = ScriptedSettingsGraphQL(listOf(viewerPage))
            val state = stateWith(client, store)
            assertEquals(listOf("Stale", "Names"), state.advancedScoringAnime)

            state.refresh()

            assertEquals(listOf("Story", "Animation"), state.advancedScoringAnime)
            assertEquals(listOf("Art"), state.advancedScoringManga)
            assertEquals(
                "Story\u001FAnimation",
                store.getString("advanced_scoring_anime_names"),
            )
        } finally {
            tearDown()
        }
    }

    @Test
    fun clearSyncedResetsSyncedPrefsAndLeavesLocalOnes() {
        setUp()
        try {
            val store = FakeSettingsStore()
            val state = stateWith(ScriptedSettingsGraphQL(listOf(viewerPage)), store)

            // Non-default synced values (refresh also sets the advanced-scoring
            // names, which have no public setter) and non-default local values.
            state.refresh()
            state.startTab = StartTabOption.MANGA
            state.imageQuality = ImageQuality.LOW
            state.persistListSort = false
            state.language = AppLanguage.available.first { it.code == "es" }
            state.palette = AppPalette.OCEAN
            state.themeStyle = AppPaletteStyle.VIBRANT
            state.themeType = ThemeType.DARK
            state.font = AppFont.MANROPE
            state.surfaces = SurfaceStyle.SOLID

            state.clearSynced()

            // Synced prefs are back to their defaults, in state and in the store.
            assertEquals(TitleLanguage.ROMAJI, state.titleLanguage)
            assertEquals(StaffNameLanguage.ROMAJI_WESTERN, state.staffNameLanguage)
            assertEquals(false, state.displayAdultContent)
            assertEquals(ScoreFormat.POINT_10_DECIMAL, state.scoreFormat)
            assertEquals(ListSort.UPDATED, state.listSort)
            assertEquals(true, state.splitCompletedAnime)
            assertEquals(false, state.splitCompletedManga)
            assertEquals(false, state.advancedScoring)
            assertEquals(emptyList(), state.advancedScoringAnime)
            assertEquals(emptyList(), state.advancedScoringManga)
            assertEquals(NotificationPreferences(), state.notifications)
            assertEquals("ROMAJI", store.getString("title_language"))
            assertEquals("ROMAJI_WESTERN", store.getString("staff_name_language"))
            assertEquals(false, store.getBoolean("display_adult_content", true))
            assertEquals("POINT_10_DECIMAL", store.getString("score_format"))
            assertEquals("UPDATED", store.getString("list_sort"))
            assertEquals(true, store.getBoolean("split_completed_anime", false))
            assertEquals(false, store.getBoolean("split_completed_manga", true))
            assertEquals(false, store.getBoolean("advanced_scoring", true))
            assertEquals("", store.getString("advanced_scoring_anime_names"))
            assertEquals("", store.getString("advanced_scoring_manga_names"))
            assertEquals(true, store.getBoolean("notification_media", false))

            // Local prefs are untouched, in state and in the store.
            assertEquals(StartTabOption.MANGA, state.startTab)
            assertEquals("MANGA", store.getString(SettingsStoreKeys.StartTab))
            assertEquals(ImageQuality.LOW, state.imageQuality)
            assertEquals("LOW", store.getString("image_quality"))
            assertEquals(false, state.persistListSort)
            assertEquals(false, store.getBoolean("persist_list_sort", true))

            // App appearance prefs are local-only too: logout never resets them.
            assertEquals("es", state.language.code)
            assertEquals(AppPalette.OCEAN, state.palette)
            assertEquals(AppPaletteStyle.VIBRANT, state.themeStyle)
            assertEquals(ThemeType.DARK, state.themeType)
            assertEquals(AppFont.MANROPE, state.font)
            assertEquals(SurfaceStyle.SOLID, state.surfaces)
            assertEquals("es", store.getString(SettingsStoreKeys.Language))
            assertEquals("ocean", store.getString(SettingsStoreKeys.ThemePalette))
            assertEquals("vibrant", store.getString(SettingsStoreKeys.ThemeStyle))
            assertEquals("DARK", store.getString(SettingsStoreKeys.ThemeMode))
            assertEquals("MANROPE", store.getString(SettingsStoreKeys.Font))
            assertEquals("SOLID", store.getString(SettingsStoreKeys.SurfaceStyle))
        } finally {
            tearDown()
        }
    }

    @Test
    fun appAppearancePrefsHydrateFromTheirExistingStoreKeys() {
        setUp()
        try {
            val store = FakeSettingsStore().apply {
                preload(SettingsStoreKeys.Language, "es")
                preload(SettingsStoreKeys.ThemePalette, "ocean")
                preload(SettingsStoreKeys.ThemeStyle, "vibrant")
                preload(SettingsStoreKeys.ThemeMode, "DARK")
                preload(SettingsStoreKeys.Font, "MANROPE")
                preload(SettingsStoreKeys.SurfaceStyle, "SOLID")
            }
            val state = stateWith(ScriptedSettingsGraphQL(emptyList()), store)

            assertEquals("es", state.language.code)
            assertEquals(AppPalette.OCEAN, state.palette)
            assertEquals(AppPaletteStyle.VIBRANT, state.themeStyle)
            assertEquals(ThemeType.DARK, state.themeType)
            assertEquals(AppFont.MANROPE, state.font)
            assertEquals(SurfaceStyle.SOLID, state.surfaces)
        } finally {
            tearDown()
        }
    }

    @Test
    fun unknownOrMissingAppAppearancePrefsFallBackToDefaults() {
        setUp()
        try {
            val absent = stateWith(ScriptedSettingsGraphQL(emptyList()), FakeSettingsStore())
            assertEquals(AppLanguage.default, absent.language)
            assertEquals(AppPalette.MONO, absent.palette)
            assertEquals(AppPaletteStyle.TONAL_SPOT, absent.themeStyle)
            assertEquals(ThemeType.SYSTEM, absent.themeType)
            assertEquals(AppFont.JAKARTA, absent.font)
            assertEquals(SurfaceStyle.GLASS, absent.surfaces)

            val corrupt = FakeSettingsStore().apply {
                preload(SettingsStoreKeys.Language, "xx")
                preload(SettingsStoreKeys.ThemePalette, "nope")
                preload(SettingsStoreKeys.ThemeStyle, "nope")
                preload(SettingsStoreKeys.ThemeMode, "NOPE")
                preload(SettingsStoreKeys.Font, "NOPE")
                preload(SettingsStoreKeys.SurfaceStyle, "NOPE")
            }
            val state = stateWith(ScriptedSettingsGraphQL(emptyList()), corrupt)

            assertEquals(AppLanguage.default, state.language)
            assertEquals(AppPalette.MONO, state.palette)
            assertEquals(AppPaletteStyle.TONAL_SPOT, state.themeStyle)
            assertEquals(ThemeType.SYSTEM, state.themeType)
            assertEquals(AppFont.JAKARTA, state.font)
            assertEquals(SurfaceStyle.GLASS, state.surfaces)
        } finally {
            tearDown()
        }
    }

    @Test
    fun appAppearanceSettersPersistWithTheAppsStoreFormat() {
        setUp()
        try {
            val store = FakeSettingsStore()
            val state = stateWith(ScriptedSettingsGraphQL(emptyList()), store)

            state.language = AppLanguage.available.first { it.code == "es" }
            assertEquals("es", store.getString(SettingsStoreKeys.Language))

            state.palette = AppPalette.OCEAN
            assertEquals("ocean", store.getString(SettingsStoreKeys.ThemePalette))

            state.themeStyle = AppPaletteStyle.VIBRANT
            assertEquals("vibrant", store.getString(SettingsStoreKeys.ThemeStyle))

            state.themeType = ThemeType.DARK
            assertEquals("DARK", store.getString(SettingsStoreKeys.ThemeMode))

            state.font = AppFont.MANROPE
            assertEquals("MANROPE", store.getString(SettingsStoreKeys.Font))

            state.surfaces = SurfaceStyle.SOLID
            assertEquals("SOLID", store.getString(SettingsStoreKeys.SurfaceStyle))
        } finally {
            tearDown()
        }
    }
}
