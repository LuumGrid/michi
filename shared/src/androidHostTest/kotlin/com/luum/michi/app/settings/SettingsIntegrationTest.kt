package com.luum.michi.app.settings

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.settings.domain.model.ListSort
import com.luum.michi.app.settings.domain.model.ScoreFormat
import com.luum.michi.app.settings.domain.model.StaffNameLanguage
import com.luum.michi.app.settings.domain.model.TitleLanguage
import com.luum.michi.app.settings.repository.SettingsRepositoryImpl
import com.luum.michi.app.settings.ui.state.SettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
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
            })
            put("mangaList", buildJsonObject {
                put("splitCompletedSectionByFormat", true)
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
}
