package com.luum.michi.app.ui

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberFilterSettings(): FilterSettings

internal interface FilterSettings {
    fun saveFilter(sort: String, order: String, persist: Boolean)
    fun loadFilter(): Triple<String, String, Boolean>?
}
