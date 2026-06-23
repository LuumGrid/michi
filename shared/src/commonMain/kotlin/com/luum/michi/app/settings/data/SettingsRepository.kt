package com.luum.michi.app.settings.data

import com.luum.michi.app.core.network.NetworkResult

internal interface SettingsRepository {
    suspend fun loadSettings(): NetworkResult<SettingsData>
    suspend fun saveSettings(data: SettingsData): NetworkResult<Unit>
}
