package com.luum.michi.app.settings

import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkError
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.storage.domain.SettingsStore
import com.luum.michi.app.settings.domain.SettingsRepository
import com.luum.michi.app.settings.domain.model.SettingsData
import kotlinx.serialization.json.JsonElement

/** Shared fakes for settings tests (single definition on purpose). */

/** In-memory [SettingsStore], optionally preloaded. */
internal class FakeSettingsStore : SettingsStore {
    private val values = mutableMapOf<String, Any?>()

    fun preload(key: String, value: Any?) {
        values[key] = value
    }

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getString(key: String): String? = values[key] as? String

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        values[key] as? Boolean ?: default
}

/** Scripted [SettingsRepository] with call counters. */
internal class FakeSettingsRepository : SettingsRepository {
    var loadResult: NetworkResult<SettingsData> =
        NetworkResult.Failure(NetworkError.NoConnection)
    var loadCallCount = 0
    var saveResult: NetworkResult<Unit> = NetworkResult.Success(Unit)
    var saveCallCount = 0
    var lastSaved: SettingsData? = null

    override suspend fun loadSettings(): NetworkResult<SettingsData> {
        loadCallCount++
        return loadResult
    }

    override suspend fun saveSettings(data: SettingsData): NetworkResult<Unit> {
        saveCallCount++
        lastSaved = data
        return saveResult
    }
}

/** Scripted GraphQL client: canned pages + failure index + request capture. */
internal class ScriptedSettingsGraphQL(
    private val pages: List<JsonElement>,
    private val failureAt: Int = -1,
) : AniListGraphQLClient {
    var calls = 0
    val requests = mutableListOf<AniListGraphQLRequest>()

    override suspend fun <T> execute(
        request: AniListGraphQLRequest,
        parseData: (JsonElement) -> T,
    ): NetworkResult<T> {
        val index = calls++
        requests += request
        if (index == failureAt) return NetworkResult.Failure(NetworkError.Http(500, null))
        return NetworkResult.Success(parseData(pages[index]))
    }
}
