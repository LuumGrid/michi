package com.luum.michi.app.core.network.repository

import com.luum.michi.app.core.model.ExternalLink
import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.ExternalLinkCatalogRepository
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.storage.domain.SettingsStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

private const val CatalogQuery = """
query ExternalLinkCatalog {
  ExternalLinkSourceCollection {
    site
    url
    icon
    color
    type
    language
  }
}
"""

private const val CatalogKey = "external_link_catalog"

/** 7 days: the site list is essentially static. */
private const val CatalogTtlMillis = 7L * 24 * 60 * 60 * 1000

@Serializable
private data class CatalogResponseDto(
    @SerialName("ExternalLinkSourceCollection") val sites: List<CatalogSiteDto> = emptyList(),
)

@Serializable
private data class CatalogSiteDto(
    val site: String? = null,
    val url: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val type: String? = null,
    val language: String? = null,
)

@Serializable
private data class CachedCatalogDto(
    val savedAtMillis: Long,
    val sites: List<CatalogSiteDto> = emptyList(),
)

internal class ExternalLinkCatalogRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val store: SettingsStore,
    private val json: Json = AniListJson,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ExternalLinkCatalogRepository {

    private val mutex = Mutex()
    private var memory: List<ExternalLink>? = null

    override suspend fun sites(): List<ExternalLink> {
        memory?.let { return it }
        // Serialize so concurrent callers share a single fetch.
        return mutex.withLock {
            memory?.let { return it }
            readCache()?.let { cached ->
                memory = cached
                return cached
            }
            val fetched = fetch()
            if (fetched != null) {
                memory = fetched
                writeCache(fetched)
            }
            fetched.orEmpty()
        }
    }

    private suspend fun fetch(): List<ExternalLink>? {
        val request = AniListGraphQLRequest(query = CatalogQuery, operationName = "ExternalLinkCatalog")
        return when (val result = graphQLClient.execute(request) { data ->
            AniListJson.decodeFromJsonElement(CatalogResponseDto.serializer(), data)
        }) {
            is NetworkResult.Success -> result.value.sites.mapNotNull { it.toExternalLink() }
            is NetworkResult.Failure -> null
        }
    }

    private fun readCache(): List<ExternalLink>? {
        val raw = store.getString(CatalogKey)?.takeIf { it.isNotBlank() } ?: return null
        val dto = runCatching { json.decodeFromString<CachedCatalogDto>(raw) }.getOrNull() ?: return null
        if (now() - dto.savedAtMillis > CatalogTtlMillis) return null
        return dto.sites.mapNotNull { it.toExternalLink() }
    }

    private fun writeCache(sites: List<ExternalLink>) {
        val dto = CachedCatalogDto(
            savedAtMillis = now(),
            sites = sites.map { CatalogSiteDto(it.site, it.url, it.iconUrl, it.colorHex, it.type, it.language) },
        )
        store.putString(CatalogKey, json.encodeToString(dto))
    }
}

private fun CatalogSiteDto.toExternalLink(): ExternalLink? {
    val link = url?.takeIf { it.isNotBlank() } ?: return null
    return ExternalLink(
        site = site.orEmpty(),
        url = link,
        iconUrl = icon,
        colorHex = color,
        type = type,
        language = language,
    )
}
