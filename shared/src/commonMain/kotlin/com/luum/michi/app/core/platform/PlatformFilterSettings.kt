package com.luum.michi.app.core.platform

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberPlatformFilterSettings(): PlatformFilterSettings

internal interface PlatformFilterSettings {
    fun saveFilter(sort: String, order: String, persist: Boolean)
    fun loadFilter(): Triple<String, String, Boolean>?
}
