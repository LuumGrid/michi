package com.luum.michi.app

import androidx.compose.ui.window.ComposeUIViewController
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.luum.michi.app.core.auth.NSUserDefaultsAniListTokenStorage
import com.luum.michi.app.core.auth.SafariOAuthLauncher
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIViewController

/**
 * iOS entry point. Constructs the shared `MichiDependencies` and presents the
 * Compose UI.
 *
 * The OAuth callback (`michi://oauth/callback`) is delivered from SwiftUI via
 * `.onOpenURL { url in MainViewControllerKt.handleIosOAuthCallback(url: url.absoluteString) }`.
 * The matching URL scheme is declared in `Info.plist` under `CFBundleURLTypes`.
 */
@Suppress("FunctionName") // referenced from Swift (iOSApp.swift / ContentView.swift)
fun MainViewController(): UIViewController {
    configureCoilSingleton()
    val dependencies = MichiDependencies(
        tokenStorage = NSUserDefaultsAniListTokenStorage(),
        oAuthLauncher = SafariOAuthLauncher(),
    )
    IosMichiDependencies.bind(dependencies)
    return ComposeUIViewController { App(dependencies = dependencies) }
}

/**
 * Configure the Coil singleton ImageLoader with memory + disk cache for iOS.
 * Called once before the first Compose frame.
 */
private fun configureCoilSingleton() {
    SingletonImageLoader.setSafe(
        SingletonImageLoader.Factory { context: PlatformContext ->
            val cacheDir = (
                NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
                    .firstOrNull() as? String
            )?.let { "$it/michi_image_cache".toPath() }

            ImageLoader.Builder(context)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(context, percent = 0.25)
                        .build()
                }
                .diskCache {
                    val builder = DiskCache.Builder()
                        .maxSizeBytes(128L * 1024 * 1024)
                    if (cacheDir != null) builder.directory(cacheDir) else builder
                    builder.build()
                }
                .crossfade(true)
                .build()
        },
    )
}

/** Called from Swift via `MainViewControllerKt.handleIosOAuthCallback(url: ...)`. */
fun handleIosOAuthCallback(url: String) {
    IosMichiDependencies.current?.onOAuthCallback(url)
}

private object IosMichiDependencies {
    var current: MichiDependencies? = null
        private set

    fun bind(dependencies: MichiDependencies) {
        current = dependencies
    }
}
