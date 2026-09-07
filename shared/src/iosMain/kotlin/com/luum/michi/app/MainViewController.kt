package com.luum.michi.app

import com.luum.michi.app.core.repository.auth.NSUserDefaultsAniListTokenStorage
import com.luum.michi.app.core.repository.auth.SafariOAuthLauncher
import platform.UIKit.UIViewController

/**
 * UI placeholder: preserves the iOS entry point + OAuth callback contract
 * used from Swift (`ContentView.swift` / `iOSApp.swift`) without any Compose
 * UI. Screens will be re-added on top of the pure-logic state holders.
 */
@Suppress("FunctionName") // referenced from Swift (iOSApp.swift / ContentView.swift)
fun MainViewController(): UIViewController {
    IosMichiDependencies.getOrCreate()
    return UIViewController()
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
