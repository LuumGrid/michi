@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.luum.michi.app.core.storage.repository

import com.luum.michi.app.core.storage.domain.FileStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/** One cache folder for every user-scoped file (lists today, profile later). */
private const val CacheDirName = "michi_cache"

/** File work runs on [Dispatchers.IO] (a shared pool for blocking callers). */
private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

/**
 * [FileStore] over `NSCachesDirectory`, in its own subfolder. Every call runs on
 * [ioDispatcher]; writes are atomic (`writeToFile(atomically = true)`), so a
 * killed process never leaves a half-written file.
 */
internal class IosFileStore : FileStore {

    private val dir: String = (
        NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String
        ).orEmpty() + "/" + CacheDirName

    private fun path(name: String): String = "$dir/$name"

    override suspend fun read(name: String): String? = withContext(ioDispatcher) {
        val file = path(name)
        if (!NSFileManager.defaultManager.fileExistsAtPath(file)) return@withContext null
        NSString.stringWithContentsOfFile(file, NSUTF8StringEncoding, null)
    }

    override suspend fun write(name: String, content: String) {
        withContext(ioDispatcher) {
            NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
            @Suppress("CAST_NEVER_SUCCEEDS")
            (content as NSString).writeToFile(path(name), true, NSUTF8StringEncoding, null)
        }
    }

    override suspend fun delete(name: String) {
        withContext(ioDispatcher) {
            NSFileManager.defaultManager.removeItemAtPath(path(name), null)
        }
    }

    override suspend fun clear() {
        withContext(ioDispatcher) {
            val manager = NSFileManager.defaultManager
            val names = manager.contentsOfDirectoryAtPath(dir, null) ?: return@withContext
            names.forEach { entry ->
                (entry as? String)?.let { manager.removeItemAtPath(path(it), null) }
            }
        }
    }
}
