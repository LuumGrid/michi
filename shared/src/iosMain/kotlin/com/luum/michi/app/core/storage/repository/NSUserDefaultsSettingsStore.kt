package com.luum.michi.app.core.storage.repository

import platform.Foundation.NSUserDefaults
import com.luum.michi.app.core.storage.domain.SettingsStore

/**
 * Local app prefs (`SettingsStore`) in `NSUserDefaults`. Synchronous by
 * design: small strings/booleans read at startup, written on every change.
 */
internal class NSUserDefaultsSettingsStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : SettingsStore {

    override fun putString(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default
}
