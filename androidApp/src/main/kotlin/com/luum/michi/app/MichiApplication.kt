package com.luum.michi.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okhttp3.OkHttpClient

/**
 * Application class that configures the Coil singleton ImageLoader with:
 * - Memory cache (25% of available heap, up to ~100 MB)
 * - Disk cache (128 MB in the standard cache directory)
 * - OkHttp network fetcher for efficient HTTP handling
 * - Crossfade transition on all loads
 */
class MichiApplication : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, percent = 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(128L * 1024 * 1024) // 128 MB
                    .build()
            }
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { OkHttpClient() }))
            }
            .crossfade(true)
            .build()
    }
}
