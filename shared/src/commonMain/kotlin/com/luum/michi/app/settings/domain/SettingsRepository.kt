package com.luum.michi.app.settings.domain

import com.luum.michi.app.core.network.NetworkResult
import com.luum.michi.app.settings.domain.model.SettingsData

internal interface SettingsRepository {
    suspend fun loadSettings(): NetworkResult<SettingsData>
    suspend fun saveSettings(data: SettingsData): NetworkResult<Unit>
}
