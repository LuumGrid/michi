package com.luum.michi.app.settings

import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.settings.domain.model.ListSort
import com.luum.michi.app.settings.domain.model.NotificationPreferences
import com.luum.michi.app.settings.domain.model.ScoreFormat
import com.luum.michi.app.settings.domain.model.SettingsData
import com.luum.michi.app.settings.domain.model.TitleLanguage
import com.luum.michi.app.settings.ui.state.SettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private val cannedSettings = SettingsData(
    titleLanguage = TitleLanguage.NATIVE,
    scoreFormat = ScoreFormat.POINT_100,
    displayAdultContent = true,
    listSort = ListSort.SCORE,
    splitCompletedAnime = false,
    splitCompletedManga = true,
    advancedScoring = true,
    notifications = NotificationPreferences(
        airing = false,
        activity = false,
        following = false,
        forum = true,
        messages = false,
        media = false,
    ),
)

class SettingsStateTest {

    private lateinit var scope: CoroutineScope

    private fun setUp() {
        scope = CoroutineScope(Dispatchers.Unconfined)
    }

    private fun tearDown() {
        scope.cancel()
    }

    @Test
    fun hydratesFromStoreWithoutTouchingNetwork() {
        setUp()
        try {
            val store = FakeSettingsStore().apply {
                preload("title_language", "NATIVE")
                preload("display_adult_content", true)
                preload("score_format", "POINT_100")
                preload("list_sort", "SCORE")
                preload("split_completed_anime", false)
                preload("advanced_scoring", true)
                preload("notification_forum", true)
            }
            val repository = FakeSettingsRepository()

            val state = SettingsState(repository, store, scope)

            assertEquals(TitleLanguage.NATIVE, state.titleLanguage)
            assertEquals(true, state.displayAdultContent)
            assertEquals(ScoreFormat.POINT_100, state.scoreFormat)
            assertEquals(ListSort.SCORE, state.listSort)
            assertEquals(false, state.splitCompletedAnime)
            assertEquals(true, state.advancedScoring)
            assertEquals(true, state.notifications.forum)
            assertNull(state.error)
            assertEquals(0, repository.loadCallCount)
        } finally {
            tearDown()
        }
    }

    @Test
    fun unknownStoredValuesFallBackToDefaults() {
        setUp()
        try {
            val store = FakeSettingsStore().apply {
                preload("title_language", "KLINGON")
                preload("score_format", "???")
                preload("list_sort", "whatever")
            }

            val state = SettingsState(FakeSettingsRepository(), store, scope)

            assertEquals(TitleLanguage.ROMAJI, state.titleLanguage)
            assertEquals(ScoreFormat.POINT_10_DECIMAL, state.scoreFormat)
            assertEquals(ListSort.UPDATED, state.listSort)
        } finally {
            tearDown()
        }
    }

    @Test
    fun settersPersistToStoreImmediately() {
        setUp()
        try {
            val store = FakeSettingsStore()
            val state = SettingsState(FakeSettingsRepository(), store, scope)

            state.titleLanguage = TitleLanguage.ENGLISH
            state.splitCompletedManga = true
            state.notifications = state.notifications.copy(airing = false)

            assertEquals("ENGLISH", store.getString("title_language"))
            assertEquals(true, store.getBoolean("split_completed_manga", false))
            assertEquals(false, store.getBoolean("notification_airing", true))
        } finally {
            tearDown()
        }
    }

    @Test
    fun refreshSuccessAppliesServerValuesAndClearsError() {
        setUp()
        try {
            val store = FakeSettingsStore()
            val repository = FakeSettingsRepository().apply {
                loadResult = NetworkResult.Success(cannedSettings)
            }
            val state = SettingsState(repository, store, scope)

            state.refresh()

            assertEquals(TitleLanguage.NATIVE, state.titleLanguage)
            assertEquals(ScoreFormat.POINT_100, state.scoreFormat)
            assertEquals(true, state.displayAdultContent)
            assertEquals(ListSort.SCORE, state.listSort)
            assertEquals(false, state.splitCompletedAnime)
            assertEquals(true, state.splitCompletedManga)
            assertEquals(true, state.advancedScoring)
            assertEquals(cannedSettings.notifications, state.notifications)
            assertNull(state.error)
            assertEquals(1, repository.loadCallCount)
            assertEquals("NATIVE", store.getString("title_language"))
        } finally {
            tearDown()
        }
    }

    @Test
    fun refreshFailureSetsErrorAndKeepsLocalValues() {
        setUp()
        try {
            val store = FakeSettingsStore()
            val repository = FakeSettingsRepository().apply {
                loadResult = NetworkResult.Failure(NetworkError.NoConnection)
            }
            val state = SettingsState(repository, store, scope)

            state.refresh()

            assertIs<NetworkError.NoConnection>(state.error)
            assertEquals(TitleLanguage.ROMAJI, state.titleLanguage)
            assertEquals(1, repository.loadCallCount)

            repository.loadResult = NetworkResult.Success(cannedSettings)
            state.refresh()

            assertNull(state.error)
            assertEquals(TitleLanguage.NATIVE, state.titleLanguage)
            assertEquals(2, repository.loadCallCount)
        } finally {
            tearDown()
        }
    }

    @Test
    fun guestHolderNeverTouchesNetwork() {
        setUp()
        try {
            val store = FakeSettingsStore()
            val repository = FakeSettingsRepository().apply {
                loadResult = NetworkResult.Success(cannedSettings)
            }
            val state = SettingsState(repository, store, scope, canSync = false)

            state.refresh()

            assertEquals(0, repository.loadCallCount)
            assertEquals(TitleLanguage.ROMAJI, state.titleLanguage)
            assertNull(state.error)

            state.titleLanguage = TitleLanguage.ENGLISH

            assertEquals("ENGLISH", store.getString("title_language"))
            assertEquals(0, repository.saveCallCount)
        } finally {
            tearDown()
        }
    }
}
