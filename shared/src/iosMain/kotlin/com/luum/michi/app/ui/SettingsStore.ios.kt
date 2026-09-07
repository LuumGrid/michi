package com.luum.michi.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberSettingsStore(): SettingsStore {
    return remember {
        IosSettingsStore()
    }
}

private class IosSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun putString(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        if (defaults.objectForKey(key) == null) return default
        return defaults.boolForKey(key)
    }
}
