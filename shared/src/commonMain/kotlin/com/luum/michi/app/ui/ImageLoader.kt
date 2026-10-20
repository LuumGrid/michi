package com.luum.michi.app.ui

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import okio.FileSystem

/**
 * Coil's disk cache cap: 64MB. Coil's default is 2% of free space (10MB–250MB),
 * which let this app's cache grow to about 200MB.
 */
internal const val ImageDiskCacheMaxBytes = 64L * 1024 * 1024

/**
 * The folder Coil's default singleton disk cache uses:
 * `FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "coil3_disk_cache"`. Reusing it keeps
 * an already-populated cache instead of orphaning it; the new 64MB cap takes
 * effect as the LRU is used.
 */
private const val ImageDiskCacheDirName = "coil3_disk_cache"

/**
 * App-wide Coil loader. Crossfade softens the load, the memory cache is capped
 * to a fraction of the app heap, and the disk cache is explicitly capped at
 * [ImageDiskCacheMaxBytes] in the same folder Coil's default singleton uses
 * (its own default cap was a percentage of free space, up to 250MB). Call once
 * from [App], before the first image composes.
 */
@Composable
internal fun installImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.2)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / ImageDiskCacheDirName)
                    .maxSizeBytes(ImageDiskCacheMaxBytes)
                    .build()
            }
            .build()
    }
}
