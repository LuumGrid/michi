package com.luum.michi.app.mediaList.repository

import com.luum.michi.app.core.media.domain.MediaType
import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.storage.domain.FileStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Bump when the persisted [MediaListEntryDto] shape changes: the version travels
 * in the file name, so an old file is simply never read again (no migration).
 */
private const val ListCacheFormatVersion = 1

/** The one place the on-disk list file name is built (tests seed/corrupt with it). */
internal fun listCacheFileName(userId: Int, type: MediaType): String =
    "media_list_v${ListCacheFormatVersion}_${type.name.lowercase()}_$userId.json"

/**
 * Disk cache for one user's anime/manga list: the flat [MediaListEntryDto] list
 * exactly as fetched. It is mapped to domain on read with the **current**
 * `splitCompleted`, so flipping that setting needs no separate file. A corrupt
 * file is deleted and read as absent, and a write failure is swallowed so it can
 * never turn an otherwise-successful network load into a failure.
 *
 * JSON decode/encode is CPU-heavy for a ~1 MB list, so it runs on
 * [decodeDispatcher] ([Dispatchers.Default] in the app; tests inject a
 * synchronous one). File IO itself is off the main thread inside [FileStore].
 */
internal class MediaListDiskCache(
    private val files: FileStore,
    private val json: Json = AniListJson,
    private val decodeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val serializer = ListSerializer(MediaListEntryDto.serializer())

    suspend fun read(userId: Int, type: MediaType): List<MediaListEntryDto>? {
        val name = listCacheFileName(userId, type)
        val raw = files.read(name) ?: return null
        val decoded = withContext(decodeDispatcher) {
            try {
                json.decodeFromString(serializer, raw)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // Corrupt / undecodable: treated as absent below.
                null
            }
        }
        if (decoded == null) {
            try {
                files.delete(name)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // A failed cleanup never breaks the caller: the file is ignored.
            }
        }
        return decoded
    }

    suspend fun write(userId: Int, type: MediaType, entries: List<MediaListEntryDto>) {
        val name = listCacheFileName(userId, type)
        val raw = withContext(decodeDispatcher) { json.encodeToString(serializer, entries) }
        try {
            files.write(name, raw)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Cache-only: a full disk / missing permission must not fail the load.
        }
    }

    suspend fun clear() {
        try {
            files.clear()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Best-effort privacy wipe.
        }
    }
}
