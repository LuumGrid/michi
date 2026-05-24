package com.luum.michi.app.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberPlatformFilterSettings(): PlatformFilterSettings {
    return remember {
        IosPlatformFilterSettings()
    }
}

private class IosPlatformFilterSettings : PlatformFilterSettings {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun saveFilter(sort: String, order: String, persist: Boolean) {
        if (persist) {
            defaults.setObject(sort, "michi_sort")
            defaults.setObject(order, "michi_order")
            defaults.setBool(true, "michi_persist")
        } else {
            defaults.removeObjectForKey("michi_sort")
            defaults.removeObjectForKey("michi_order")
            defaults.removeObjectForKey("michi_persist")
        }
    }

    override fun loadFilter(): Triple<String, String, Boolean>? {
        if (!defaults.boolForKey("michi_persist")) return null
        val sort = defaults.stringForKey("michi_sort") ?: return null
        val order = defaults.stringForKey("michi_order") ?: return null
        return Triple(sort, order, true)
    }
}
