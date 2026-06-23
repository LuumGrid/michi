package com.luum.michi.app.core.platform

import androidx.compose.runtime.Composable

internal interface PlatformSettingsStore {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean): Boolean
}

@Composable
internal expect fun rememberPlatformSettingsStore(): PlatformSettingsStore
