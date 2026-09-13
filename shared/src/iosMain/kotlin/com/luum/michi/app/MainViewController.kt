package com.luum.michi.app

import androidx.compose.ui.window.ComposeUIViewController
import com.luum.michi.app.core.auth.repository.NSUserDefaultsAniListTokenStorage
import com.luum.michi.app.core.auth.repository.SafariOAuthLauncher
import platform.UIKit.UIViewController

/**
 * Compose entry: renders the shared [App] (auth landing vs shell routing).
 * Preserves the iOS entry point + OAuth callback contract used from Swift
 * (`ContentView.swift` / `iOSApp.swift`).
 */
@Suppress("FunctionName") // referenced from Swift (iOSApp.swift / ContentView.swift)
fun MainViewController(): UIViewController {
    val dependencies = IosMichiDependencies.getOrCreate()
    return ComposeUIViewController { App(dependencies = dependencies) }
}

/** Called from Swift via `MainViewControllerKt.handleIosOAuthCallback(url: ...)`. */
fun handleIosOAuthCallback(url: String) {
    IosMichiDependencies.current?.onOAuthCallback(url)
}

private object IosMichiDependencies {
    var current: MichiDependencies? = null
        private set

    /**
     * Process-lifetime holder: creates the graph once no matter how often
     * `MainViewController()` is invoked, so repeated calls neither leak the
     * previous CoroutineScope + HttpClient nor re-trigger bootstrap.
     */
    fun getOrCreate(): MichiDependencies =
        current ?: MichiDependencies(
            tokenStorage = NSUserDefaultsAniListTokenStorage(),
            oAuthLauncher = SafariOAuthLauncher(),
        ).also {
            current = it
            it.bootstrap()
        }
}
