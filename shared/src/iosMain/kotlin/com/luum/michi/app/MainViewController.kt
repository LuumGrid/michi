package com.luum.michi.app

import androidx.compose.ui.window.ComposeUIViewController
import com.luum.michi.app.core.auth.NSUserDefaultsAniListTokenStorage
import com.luum.michi.app.core.auth.SafariOAuthLauncher
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
    val dependencies = MichiDependencies(
        tokenStorage = NSUserDefaultsAniListTokenStorage(),
        oAuthLauncher = SafariOAuthLauncher(),
    )
    IosMichiDependencies.bind(dependencies)
    return ComposeUIViewController { App(dependencies = dependencies) }
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
