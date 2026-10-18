package com.luum.michi.app.profile.repository

import com.luum.michi.app.core.network.repository.AniListJson
import com.luum.michi.app.core.storage.domain.FileStore
import com.luum.michi.app.profile.domain.ProfileData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Bump when the persisted [ProfileCacheDto] shape changes: the version travels
 * in the file name, so an old file is simply never read again (no migration).
 */
private const val ProfileCacheFormatVersion = 1

/** The one place the on-disk profile file name is built (tests seed/corrupt with it). */
internal fun profileCacheFileName(userId: Int): String =
    "profile_v${ProfileCacheFormatVersion}_$userId.json"

/**
 * Disk cache for one user's profile: the whole [ProfileCacheDto] (counts,
 * distributions and favourites) exactly as fetched, in the app cache folder via
 * [FileStore] — never `SharedPreferences`. A corrupt file is deleted and read as
 * absent, and a write failure is swallowed so it can never turn an otherwise
 * successful network load into a failure. One file per user, so another user's
 * profile is never read.
 *
 * JSON decode/encode runs on [decodeDispatcher] ([Dispatchers.Default] in the
 * app; tests inject a synchronous one).
 */
internal class ProfileDiskCache(
    private val files: FileStore,
    private val json: Json = AniListJson,
    private val decodeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun read(userId: Int): ProfileData? {
        val name = profileCacheFileName(userId)
        val raw = files.read(name) ?: return null
        val decoded = withContext(decodeDispatcher) {
            try {
                json.decodeFromString(ProfileCacheDto.serializer(), raw).toDomain()
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

    suspend fun write(userId: Int, data: ProfileData) {
        val name = profileCacheFileName(userId)
        val raw = withContext(decodeDispatcher) {
            json.encodeToString(ProfileCacheDto.serializer(), data.toCacheDto())
        }
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
