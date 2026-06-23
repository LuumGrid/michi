package com.luum.michi.app.core.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

@Composable
internal actual fun rememberPlatformSettingsStore(): PlatformSettingsStore {
    val context = LocalContext.current
    return remember(context) {
        AndroidPlatformSettingsStore(context)
    }
}

private class AndroidPlatformSettingsStore(context: Context) : PlatformSettingsStore {
    private val prefs = context.applicationContext.getSharedPreferences("michi_settings", Context.MODE_PRIVATE)

    override fun putString(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
}
