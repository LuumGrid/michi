package com.luum.michi.app.core

import com.luum.michi.app.core.storage.domain.SettingsStoreKeys
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageKeysTest {

    @Test
    fun storeKeysAreUnique() {
        val keys = setOf(
            SettingsStoreKeys.ThemeMode,
            SettingsStoreKeys.ThemePalette,
            SettingsStoreKeys.Font,
            SettingsStoreKeys.Language,
            SettingsStoreKeys.DefaultDiscoverTab,
        )
        assertEquals(5, keys.size)
    }
}
