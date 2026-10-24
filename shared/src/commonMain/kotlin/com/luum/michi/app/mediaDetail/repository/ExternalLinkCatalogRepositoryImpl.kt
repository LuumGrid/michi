package com.luum.michi.app.mediaDetail.repository

import com.luum.michi.app.core.network.domain.AniListGraphQLClient
import com.luum.michi.app.core.network.domain.AniListGraphQLRequest
import com.luum.michi.app.core.network.domain.NetworkResult
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.storage.domain.SettingsStore
import com.luum.michi.app.mediaDetail.domain.common.ExternalLink
import com.luum.michi.app.mediaDetail.domain.common.ExternalLinkCatalogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
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

/** A failed fetch is not retried for 10 minutes (rate-limit friendly). */
private const val CatalogFailureTtlMillis = 10L * 60 * 1000

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

/**
 * [ExternalLinkCatalogRepository] over the AniList `ExternalLinkSourceCollection`
 * query. Memory + disk cache; one shared in-flight request (created under the
 * [Mutex], run in [scope] so it belongs to the catalog, not to a detail call).
 */
internal class ExternalLinkCatalogRepositoryImpl(
    private val graphQLClient: AniListGraphQLClient,
    private val store: SettingsStore,
    private val scope: CoroutineScope,
    private val json: Json = AniListJson,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ExternalLinkCatalogRepository {

    private val mutex = Mutex()
    private var memory: List<ExternalLink>? = null
    private var inFlight: Deferred<List<ExternalLink>>? = null
    private var failureAtMillis: Long? = null

    override fun prefetch() {
        if (memory != null || inFlight?.isActive == true) return
        // This path cannot suspend, so it cannot `withLock`; `tryLock` keeps the
        // Mutex as the single creation point (a lost race means another caller
        // is already creating it).
        if (mutex.tryLock()) {
            try {
                if (inFlight?.isActive != true) inFlight = scope.async { fetchAndCache() }
            } finally {
                mutex.unlock()
            }
        }
    }

    override suspend fun sites(): List<ExternalLink> {
        memory?.let { return it }
        val deferred = mutex.withLock {
            memory?.let { return it }
            inFlight?.takeIf { it.isActive } ?: scope.async { fetchAndCache() }.also { inFlight = it }
        }
        return deferred.await()
    }

    /** Memory → disk → network; a failure is remembered for [CatalogFailureTtlMillis]. */
    private suspend fun fetchAndCache(): List<ExternalLink> {
        memory?.let { return it }
        readCache()?.let { cached ->
            memory = cached
            return cached
        }
        val failedAt = failureAtMillis
        if (failedAt != null && now() - failedAt < CatalogFailureTtlMillis) return emptyList()
        val fetched = fetch()
        return if (fetched != null) {
            failureAtMillis = null
            memory = fetched
            writeCache(fetched)
            fetched
        } else {
            failureAtMillis = now()
            emptyList()
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
