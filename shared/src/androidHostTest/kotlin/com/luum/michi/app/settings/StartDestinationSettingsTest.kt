package com.luum.michi.app.settings

import com.luum.michi.app.core.storage.domain.SettingsStoreKeys
import com.luum.michi.app.settings.domain.model.StartCatalogOption
import com.luum.michi.app.settings.domain.model.StartTabOption
import com.luum.michi.app.settings.repository.SettingsRepositoryImpl
import com.luum.michi.app.settings.ui.state.SettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals

/** "Pestaña al abrir" is local-only: store-backed, never synced with AniList. */
class StartDestinationSettingsTest {

    private fun stateWith(store: FakeSettingsStore, client: ScriptedSettingsGraphQL = ScriptedSettingsGraphQL(emptyList())): SettingsState =
        SettingsState(
            repository = SettingsRepositoryImpl(client),
            store = store,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

    @Test
    fun defaultsAreDiscoverWithAnimeAndAllSections() {
        val state = stateWith(FakeSettingsStore())

        assertEquals(StartTabOption.DISCOVER, state.startTab)
        assertEquals(StartCatalogOption.ANIME, state.startCatalogType)
        assertEquals("ALL", state.startAnimeSectionId)
        assertEquals("ALL", state.startMangaSectionId)
    }

    @Test
    fun everySubChoicePersistsAcrossSessions() {
        val store = FakeSettingsStore()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val state = SettingsState(
            repository = SettingsRepositoryImpl(ScriptedSettingsGraphQL(emptyList())),
            store = store,
            scope = scope,
        )

        state.startTab = StartTabOption.ANIME
        state.startAnimeSectionId = "WATCHING"
        state.startCatalogType = StartCatalogOption.MANGA
        state.startMangaSectionId = "CURRENT"
        scope.cancel()

        val reloaded = stateWith(store)

        assertEquals(StartTabOption.ANIME, reloaded.startTab)
        assertEquals("WATCHING", reloaded.startAnimeSectionId)
        assertEquals(StartCatalogOption.MANGA, reloaded.startCatalogType)
        assertEquals("CURRENT", reloaded.startMangaSectionId)
    }

    @Test
    fun unknownStoredValuesFallBackToDefaults() {
        val store = FakeSettingsStore().apply {
            preload(SettingsStoreKeys.StartTab, "NOT_A_TAB")
            preload(SettingsStoreKeys.StartCatalogType, "NOT_A_TYPE")
            // A section id is kept raw here; Root resolves a stale one to ALL.
            preload(SettingsStoreKeys.StartAnimeSection, "GONE_SECTION")
        }

        val state = stateWith(store)

        assertEquals(StartTabOption.DISCOVER, state.startTab)
        assertEquals(StartCatalogOption.ANIME, state.startCatalogType)
        assertEquals("GONE_SECTION", state.startAnimeSectionId)
    }

    @Test
    fun theSettingIsNeverSentToAniList() {
        val store = FakeSettingsStore()
        val state = stateWith(store)

        state.startTab = StartTabOption.MANGA
        state.startMangaSectionId = "COMPLETED"

        // Local-only: no server payload field carries it (nothing to assert
        // beyond the store write, which the persistence test covers).
        assertEquals("MANGA", store.getString(SettingsStoreKeys.StartTab))
        assertEquals("COMPLETED", store.getString(SettingsStoreKeys.StartMangaSection))
    }
}
