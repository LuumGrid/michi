package com.luum.michi.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

@Composable
internal actual fun rememberFilterSettings(): FilterSettings {
    val context = LocalContext.current
    return remember(context) {
        AndroidFilterSettings(context)
    }
}

private class AndroidFilterSettings(context: Context) : FilterSettings {
    private val prefs = context.applicationContext.getSharedPreferences("michi_list_filters", Context.MODE_PRIVATE)

    override fun saveFilter(sort: String, order: String, persist: Boolean) {
        if (persist) {
            prefs.edit {
                putString("sort", sort)
                    .putString("order", order)
                    .putBoolean("persist", true)
            }
        } else {
            prefs.edit { clear() }
        }
    }

    override fun loadFilter(): Triple<String, String, Boolean>? {
        if (!prefs.getBoolean("persist", false)) return null
        val sort = prefs.getString("sort", null) ?: return null
        val order = prefs.getString("order", null) ?: return null
        return Triple(sort, order, true)
    }
}
