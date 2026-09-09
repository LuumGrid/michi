package com.luum.michi.app.core

import com.luum.michi.app.core.storage.domain.SettingsStoreKeys
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageKeysTest {

    @Test
    fun storeKeysAreUnique() {
        val keys = setOf(
            SettingsStoreKeys.ThemeMode,
            SettingsStoreKeys.Language,
            SettingsStoreKeys.DefaultDiscoverTab,
        )
        assertEquals(3, keys.size)
    }
}
