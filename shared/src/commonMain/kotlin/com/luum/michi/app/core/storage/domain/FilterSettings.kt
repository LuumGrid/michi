package com.luum.michi.app.core.storage.domain

internal interface FilterSettings {
    fun saveFilter(sort: String, order: String, persist: Boolean)
    fun loadFilter(): Triple<String, String, Boolean>?
}
