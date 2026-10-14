package com.luum.michi.app.discover.repository

import com.luum.michi.app.core.model.CoverSizes
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.storage.domain.SettingsStore
import com.luum.michi.app.discover.domain.CachedDashboardFeed
import com.luum.michi.app.discover.domain.DashboardFeed
import com.luum.michi.app.discover.domain.DashboardFeedCache
import com.luum.michi.app.discover.domain.model.DashboardMediaType
import com.luum.michi.app.discover.domain.model.DashboardSectionKind
import com.luum.michi.app.discover.domain.model.MediaItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Serializable
private data class CachedDashboardDto(
    val savedAtMillis: Long,
    val sections: List<CachedSectionDto> = emptyList(),
)

@Serializable
private data class CachedSectionDto(
    val kind: String,
    val items: List<CachedItemDto> = emptyList(),
)

@Serializable
private data class CachedItemDto(
    val title: String,
    val meta: String,
    val id: Int? = null,
    val medium: String? = null,
    val large: String? = null,
    val extraLarge: String? = null,
    val placeholderHex: String? = null,
)

/** [DashboardFeedCache] over the key/value [SettingsStore] (no new dependency). */
internal class SettingsStoreDashboardFeedCache(
    private val store: SettingsStore,
    private val json: Json = AniListJson,
) : DashboardFeedCache {

    override fun load(mediaType: DashboardMediaType, hideAdult: Boolean): CachedDashboardFeed? =
        store.getString(key(mediaType, hideAdult))
            ?.takeIf { it.isNotBlank() }
            ?.let { raw -> runCatching { json.decodeFromString<CachedDashboardDto>(raw) }.getOrNull() }
            ?.let { dto ->
                CachedDashboardFeed(
                    savedAtMillis = dto.savedAtMillis,
                    feed = DashboardFeed(
                        sections = dto.sections.mapNotNull { section ->
                            val kind = DashboardSectionKind.entries.firstOrNull { it.name == section.kind }
                                ?: return@mapNotNull null
                            kind to section.items.map { item ->
                                MediaItem(
                                    title = item.title,
                                    meta = item.meta,
                                    id = item.id,
                                    cover = if (item.medium == null && item.large == null && item.extraLarge == null) {
                                        null
                                    } else {
                                        CoverSizes(medium = item.medium, large = item.large, extraLarge = item.extraLarge)
                                    },
                                    placeholderHex = item.placeholderHex,
                                )
                            }
                        }.toMap(),
                    ),
                )
            }

    override fun save(mediaType: DashboardMediaType, hideAdult: Boolean, feed: DashboardFeed) {
        val dto = CachedDashboardDto(
            savedAtMillis = Clock.System.now().toEpochMilliseconds(),
            sections = feed.sections.map { (kind, items) ->
                CachedSectionDto(
                    kind = kind.name,
                    items = items.map { item ->
                        CachedItemDto(
                            title = item.title,
                            meta = item.meta,
                            id = item.id,
                            medium = item.cover?.medium,
                            large = item.cover?.large,
                            extraLarge = item.cover?.extraLarge,
                            placeholderHex = item.placeholderHex,
                        )
                    },
                )
            },
        )
        store.putString(key(mediaType, hideAdult), json.encodeToString(dto))
    }

    override fun clear() {
        DashboardMediaType.entries.forEach { type ->
            store.putString(key(type, hideAdult = true), "")
            store.putString(key(type, hideAdult = false), "")
        }
    }

    private fun key(mediaType: DashboardMediaType, hideAdult: Boolean): String =
        "dashboard_${mediaType.name}_$hideAdult"
}
