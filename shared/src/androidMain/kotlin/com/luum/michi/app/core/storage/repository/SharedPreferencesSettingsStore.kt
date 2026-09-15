package com.luum.michi.app.core.storage.repository

import android.content.Context
import android.content.SharedPreferences
import com.luum.michi.app.core.storage.domain.SettingsStore

private const val PrefsName = "michi_settings"

/**
 * Local app prefs (`SettingsStore`) in `SharedPreferences`. Synchronous by
 * design: small strings/booleans read at startup, written on every change.
 */
internal class SharedPreferencesSettingsStore(
    context: Context,
) : SettingsStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)
}
