package com.luum.michi.app.ui

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.request.crossfade

/**
 * App-wide Coil loader. Crossfade softens the load and the memory cache is
 * capped to a fraction of the app heap, so high-resolution covers
 * (`extraLarge`, now the app default) never balloon it. Disk cache stays
 * platform-default. Call once from [App], before the first image composes.
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
            .build()
    }
}
