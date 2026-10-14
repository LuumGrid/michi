package com.luum.michi.app.core.session.repository

import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.session.domain.CachedViewerStore
import com.luum.michi.app.core.session.domain.Viewer
import com.luum.michi.app.core.storage.domain.SettingsStore
import com.luum.michi.app.core.storage.domain.SettingsStoreKeys
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class CachedViewerDto(
    val id: Int,
    val name: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val titleLanguage: String? = null,
    val staffNameLanguage: String? = null,
    val displayAdultContent: Boolean = false,
    val scoreFormat: String? = null,
)

/** [CachedViewerStore] over the key/value [SettingsStore] (no new dependency). */
internal class SettingsStoreCachedViewerStore(
    private val store: SettingsStore,
    private val json: Json = AniListJson,
) : CachedViewerStore {

    override fun load(): Viewer? =
        store.getString(SettingsStoreKeys.CachedViewer)
            ?.takeIf { it.isNotBlank() }
            ?.let { raw -> runCatching { json.decodeFromString<CachedViewerDto>(raw) }.getOrNull() }
            ?.let { dto ->
                Viewer(
                    id = dto.id,
                    name = dto.name,
                    avatarUrl = dto.avatarUrl,
                    bannerUrl = dto.bannerUrl,
                    titleLanguage = dto.titleLanguage,
                    staffNameLanguage = dto.staffNameLanguage,
                    displayAdultContent = dto.displayAdultContent,
                    scoreFormat = dto.scoreFormat,
                )
            }

    override fun save(viewer: Viewer) {
        store.putString(
            SettingsStoreKeys.CachedViewer,
            json.encodeToString(
                CachedViewerDto(
                    id = viewer.id,
                    name = viewer.name,
                    avatarUrl = viewer.avatarUrl,
                    bannerUrl = viewer.bannerUrl,
                    titleLanguage = viewer.titleLanguage,
                    staffNameLanguage = viewer.staffNameLanguage,
                    displayAdultContent = viewer.displayAdultContent,
                    scoreFormat = viewer.scoreFormat,
                ),
            ),
        )
    }

    override fun clear() {
        store.putString(SettingsStoreKeys.CachedViewer, "")
    }
}
